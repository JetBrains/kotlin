/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.caches.resolve

import com.intellij.ide.highlighter.JavaClassFileType
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.libraries.LibraryUtil
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.ClassFileViewProvider
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.compiled.ClsClassImpl
import com.intellij.psi.impl.compiled.ClsFileImpl
import com.intellij.psi.impl.java.stubs.PsiJavaFileStub
import com.intellij.psi.impl.java.stubs.impl.PsiJavaFileStubImpl
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.PsiClassHolderFileStub
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.cls.ClsFormatException
import org.jetbrains.kotlin.asJava.*
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.idea.decompiler.navigation.JetSourceNavigationHelper
import org.jetbrains.kotlin.idea.stubindex.JetFullClassNameIndex
import org.jetbrains.kotlin.idea.stubindex.JetSourceFilterScope.kotlinSourceAndClassFiles
import org.jetbrains.kotlin.idea.stubindex.JetSourceFilterScope.kotlinSourcesAndLibraries
import org.jetbrains.kotlin.idea.stubindex.JetTopLevelClassByPackageIndex
import org.jetbrains.kotlin.idea.stubindex.PackageIndexUtil
import org.jetbrains.kotlin.idea.stubindex.StaticFacadeIndexUtil
import org.jetbrains.kotlin.idea.util.ProjectRootsUtil
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.load.kotlin.PackagePartClassUtils
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getElementTextWithContext
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.*
import org.jetbrains.kotlin.resolve.scopes.JetScope
import org.jetbrains.kotlin.utils.sure
import java.io.IOException
import java.util.*

public class IDELightClassGenerationSupport(private val project: Project) : LightClassGenerationSupport() {

    private val scopeFileComparator = JavaElementFinder.byClasspathComparator(GlobalSearchScope.allScope(project))
    private val psiManager: PsiManager = PsiManager.getInstance(project)

    override fun getContextForPackage(files: Collection<JetFile>): LightClassConstructionContext {
        assert(!files.isEmpty(), "No files in package")

        return getContextForFiles(files)
    }

    private fun getContextForFiles(files: Collection<JetFile>): LightClassConstructionContext {
        val sortedFiles = files.sortedWith(scopeFileComparator)
        val file = sortedFiles.first()
        val resolveSession = file.getResolutionFacade().getFrontendService(ResolveSession::class.java)
        forceResolvePackageDeclarations(files, resolveSession)
        return LightClassConstructionContext(resolveSession.bindingContext, resolveSession.getModuleDescriptor())
    }

    override fun getContextForClassOrObject(classOrObject: JetClassOrObject): LightClassConstructionContext {
        val resolutionFacade = classOrObject.getResolutionFacade()

        val moduleDescriptor = resolutionFacade.moduleDescriptor
        val bindingContext = resolutionFacade.analyze(classOrObject, BodyResolveMode.FULL)
        if (classOrObject.isLocal()) {
            val descriptor = bindingContext.get(BindingContext.CLASS, classOrObject)


            if (descriptor == null) {
                LOG.warn("No class descriptor in context for class: " + classOrObject.getElementTextWithContext())
                return LightClassConstructionContext(bindingContext, moduleDescriptor)
            }

            ForceResolveUtil.forceResolveAllContents<ClassDescriptor>(descriptor)

            return LightClassConstructionContext(bindingContext, moduleDescriptor)
        }

        ForceResolveUtil.forceResolveAllContents(resolutionFacade.resolveToDescriptor(classOrObject))
        return LightClassConstructionContext(bindingContext, moduleDescriptor)
    }

    override fun getContextForFacade(files: Collection<JetFile>): LightClassConstructionContext {
        assert(!files.isEmpty(), "No files in facade")

        return getContextForFiles(files)
    }

    override fun findClassOrObjectDeclarations(fqName: FqName, searchScope: GlobalSearchScope): Collection<JetClassOrObject> {
        return JetFullClassNameIndex.getInstance().get(fqName.asString(), project, kotlinSourceAndClassFiles(searchScope, project))
    }

    override fun findFilesForPackage(fqName: FqName, searchScope: GlobalSearchScope): Collection<JetFile> {
        return PackageIndexUtil.findFilesWithExactPackage(fqName, kotlinSourceAndClassFiles(searchScope, project), project)
    }

    override fun findClassOrObjectDeclarationsInPackage(
            packageFqName: FqName,
            searchScope: GlobalSearchScope
    ): Collection<JetClassOrObject> {
        return JetTopLevelClassByPackageIndex.getInstance().get(
                packageFqName.asString(), project, kotlinSourceAndClassFiles(searchScope, project))
    }

    override fun packageExists(fqName: FqName, scope: GlobalSearchScope): Boolean {
        return PackageIndexUtil.packageExists(fqName, kotlinSourceAndClassFiles(scope, project), project)
    }

    override fun getSubPackages(fqn: FqName, scope: GlobalSearchScope): Collection<FqName> {
        return PackageIndexUtil.getSubPackageFqNames(fqn, kotlinSourceAndClassFiles(scope, project), project, JetScope.ALL_NAME_FILTER)
    }

    override fun getPsiClass(classOrObject: JetClassOrObject): PsiClass? {
        val virtualFile = classOrObject.containingFile.virtualFile
        if (virtualFile != null && LibraryUtil.findLibraryEntry(virtualFile, classOrObject.project) != null) {
            if (ProjectRootsUtil.isLibraryClassFile(project, virtualFile)) {
                return getLightClassForDecompiledClassOrObject(classOrObject)
            }
            return JetSourceNavigationHelper.getOriginalClass(classOrObject)
        }
        return KotlinLightClassForExplicitDeclaration.create(psiManager, classOrObject)
    }

    override fun getPackageClasses(packageFqName: FqName, scope: GlobalSearchScope): Collection<PsiClass> {
        val result = ArrayList<PsiClass>()
        val packageClassesInfos = findPackageClassesInfos(packageFqName, scope)
        for (info in packageClassesInfos) {
            val files = PackagePartClassUtils.getFilesWithCallables(info.files)
            if (files.isEmpty()) continue

            val moduleInfo = info.moduleInfo
            if (moduleInfo is ModuleSourceInfo) {
                result.addAll(getLightClassesForPackageFacadeWithSources(packageFqName, files, moduleInfo))
            }
            else {
                result.addAll(getLightClassesForDecompiledFacadeFiles(files))
            }
        }
        return result
    }

    private fun getLightClassesForPackageFacadeWithSources(
            packageFqName: FqName,
            facadeFiles: List<JetFile>,
            moduleInfo: IdeaModuleInfo
    ): List<PsiClass> {
        val lightClassForFacade = KotlinLightClassForFacade.createForPackageFacade(psiManager, packageFqName, moduleInfo.contentScope(), facadeFiles)
        return getLightClassesForFacadeWithFiles(lightClassForFacade, facadeFiles)
    }

    private fun getLightClassesForFacadeWithFiles(
            lightClassForFacade: KotlinLightClassForFacade?,
            facadeFiles: List<JetFile>
    ): List<PsiClass> {
        if (lightClassForFacade == null) return emptyList()

        val lightClasses = ArrayList<PsiClass>()
        lightClasses.add(lightClassForFacade)
        if (facadeFiles.size() > 1) {
            lightClasses.addAll(facadeFiles.map {
                FakeLightClassForFileOfPackage(psiManager, lightClassForFacade, it)
            })
        }
        return lightClasses
    }

    override fun getFacadeClasses(facadeFqName: FqName, scope: GlobalSearchScope): Collection<PsiClass> {
        val result = ArrayList<PsiClass>()
        val facadeClassesInfos = findFacadeClassesInfos(facadeFqName, scope)
        for (info in facadeClassesInfos) {
            val files = PackagePartClassUtils.getFilesWithCallables(info.files)
            if (files.isEmpty()) continue

            val moduleInfo = info.moduleInfo
            if (moduleInfo is ModuleSourceInfo) {
                result.addAll(getLightClassesForStaticFacadeWithSources(facadeFqName, files, moduleInfo))
            }
            else {
                result.addAll(getLightClassesForDecompiledFacadeFiles(files))
            }
        }
        return result
    }

    private fun getLightClassesForStaticFacadeWithSources(
            facadeFqName: FqName,
            facadeFiles: List<JetFile>,
            moduleInfo: IdeaModuleInfo): List<PsiClass> {
        val lightClassForFacade = KotlinLightClassForFacade.createForFacade(
                psiManager, facadeFqName, moduleInfo.contentScope(), facadeFiles)
        return getLightClassesForFacadeWithFiles(lightClassForFacade, facadeFiles)
    }

    override fun findFilesForFacade(facadeFqName: FqName, scope: GlobalSearchScope): Collection<JetFile> {
        return StaticFacadeIndexUtil.findFilesForStaticFacade(facadeFqName, kotlinSourcesAndLibraries(scope, project), project)
    }

    private fun findFacadeClassesInfos(facadeFqName: FqName, scope: GlobalSearchScope): List<KotlinLightFacadeClassInfo> {
        val facadeFiles = findFilesForFacade(facadeFqName, scope)
        return groupFilesIntoFacadeLightClassesByModuleInfo(facadeFiles)
    }

    override fun resolveClassToDescriptor(classOrObject: JetClassOrObject): ClassDescriptor? {
        try {
            return classOrObject.resolveToDescriptor() as ClassDescriptor
        }
        catch (e: NoDescriptorForDeclarationException) {
            return null
        }
    }

    private fun findPackageClassesInfos(fqName: FqName, wholeScope: GlobalSearchScope): List<KotlinLightFacadeClassInfo> {
        val allFiles = findFilesForPackage(fqName, wholeScope)
        return groupFilesIntoFacadeLightClassesByModuleInfo(allFiles)
    }

    private class KotlinLightFacadeClassInfo(public val files: Collection<JetFile>, public val moduleInfo: IdeaModuleInfo)

    private class CachedJavaStub(public var modificationStamp: Long, public var javaFileStub: PsiJavaFileStubImpl)

    companion object {

        private val LOG = Logger.getInstance(IDELightClassGenerationSupport::class.java)

        private fun forceResolvePackageDeclarations(files: Collection<JetFile>, session: KotlinCodeAnalyzer) {
            for (file in files) {
                // SCRIPT: not supported
                if (file.isScript) continue

                val packageFqName = file.packageFqName

                // make sure we create a package descriptor
                val packageDescriptor = session.moduleDescriptor.getPackage(packageFqName)
                if (packageDescriptor.isEmpty()) {
                    LOG.warn("No descriptor found for package " + packageFqName + " in file " + file.name + "\n" + file.text)
                    session.forceResolveAll()
                    continue
                }

                for (declaration in file.declarations) {
                    if (declaration is JetFunction) {
                        val name = declaration.nameAsSafeName
                        val functions = packageDescriptor.memberScope.getFunctions(name, NoLookupLocation.FROM_IDE)
                        for (descriptor in functions) {
                            ForceResolveUtil.forceResolveAllContents(descriptor)
                        }
                    }
                    else if (declaration is JetProperty) {
                        val name = declaration.nameAsSafeName
                        val properties = packageDescriptor.memberScope.getProperties(name, NoLookupLocation.FROM_IDE)
                        for (descriptor in properties) {
                            ForceResolveUtil.forceResolveAllContents(descriptor)
                        }
                    }
                    else if (declaration is JetClassOrObject) {
                        // Do nothing: we are not interested in classes
                    }
                    else {
                        LOG.error("Unsupported declaration kind: " + declaration + " in file " + file.name + "\n" + file.text)
                    }
                }
            }
        }

        private fun getLightClassForDecompiledClassOrObject(decompiledClassOrObject: JetClassOrObject): PsiClass? {
            if (decompiledClassOrObject is JetEnumEntry) {
                return null
            }
            val containingJetFile = decompiledClassOrObject.getContainingJetFile()
            if (!containingJetFile.isCompiled) {
                return null
            }
            val rootLightClassForDecompiledFile = createLightClassForDecompiledKotlinFile(containingJetFile) ?: return null

            return findCorrespondingLightClass(decompiledClassOrObject, rootLightClassForDecompiledFile)
        }

        private fun findCorrespondingLightClass(
                decompiledClassOrObject: JetClassOrObject,
                rootLightClassForDecompiledFile: PsiClass): PsiClass {
            val relativeFqName = getClassRelativeName(decompiledClassOrObject)
            val iterator = relativeFqName.pathSegments().iterator()
            val base = iterator.next()
            assert(rootLightClassForDecompiledFile.name == base.asString()) { "Light class for file:\n" + decompiledClassOrObject.getContainingJetFile().virtualFile.canonicalPath + "\nwas expected to have name: " + base.asString() + "\n Actual: " + rootLightClassForDecompiledFile.name }
            var current = rootLightClassForDecompiledFile
            while (iterator.hasNext()) {
                val name = iterator.next()
                val innerClass = current.findInnerClassByName(name.asString(), false).sure {
                    "Could not find corresponding inner/nested class " + relativeFqName + " in class " + decompiledClassOrObject.fqName + "\n" + "File: " + decompiledClassOrObject.getContainingJetFile().virtualFile.name
                }
                current = innerClass
            }
            return current
        }

        private fun getClassRelativeName(decompiledClassOrObject: JetClassOrObject): FqName {
            val name = decompiledClassOrObject.nameAsName!!
            val parent = PsiTreeUtil.getParentOfType(decompiledClassOrObject, JetClassOrObject::class.java, true)
            if (parent == null) {
                assert(decompiledClassOrObject.isTopLevel())
                return FqName.topLevel(name)
            }
            return getClassRelativeName(parent).child(name)
        }

        private fun groupFilesIntoFacadeLightClassesByModuleInfo(facadeFiles: Collection<JetFile>): List<KotlinLightFacadeClassInfo> {
            val filesByInfo = facadeFiles.groupBy { it.getModuleInfo() }

            return filesByInfo.map { KotlinLightFacadeClassInfo(it.value, it.key) }
        }

        private fun getLightClassesForDecompiledFacadeFiles(filesWithCallables: List<JetFile>): List<PsiClass> {
            return filesWithCallables.filter { it.isCompiled }.map { createLightClassForDecompiledKotlinFile(it) }.filterNotNull()
        }

        private fun createLightClassForDecompiledKotlinFile(file: JetFile): KotlinLightClassForDecompiledDeclaration? {
            val virtualFile = file.virtualFile ?: return null

            val classOrObject = file.declarations.filterIsInstance<JetClassOrObject>().singleOrNull()

            val javaClsClass = createClsJavaClassFromVirtualFile(file, virtualFile, classOrObject) ?: return null
            return KotlinLightClassForDecompiledDeclaration(javaClsClass, classOrObject)
        }

        private fun createClsJavaClassFromVirtualFile(
                decompiledKotlinFile: JetFile,
                virtualFile: VirtualFile,
                decompiledClassOrObject: JetClassOrObject?): ClsClassImpl? {
            val javaFileStub = getOrCreateJavaFileStub(decompiledKotlinFile, virtualFile) ?: return null
            val manager = PsiManager.getInstance(decompiledKotlinFile.project)
            val fakeFile = object : ClsFileImpl(ClassFileViewProvider(manager, virtualFile)) {
                override fun getNavigationElement(): PsiElement {
                    if (decompiledClassOrObject != null) {
                        return decompiledClassOrObject.navigationElement.containingFile
                    }
                    return super.getNavigationElement()
                }

                override fun getStub(): PsiClassHolderFileStub<*> {
                    return javaFileStub
                }

                override fun getMirror(): PsiElement {
                    return decompiledKotlinFile
                }
            }
            fakeFile.isPhysical = false
            javaFileStub.setPsi(fakeFile)
            return fakeFile.classes.single() as ClsClassImpl
        }

        private val cachedJavaStubKey = Key.create<CachedJavaStub>("CACHED_JAVA_STUB")

        private fun getOrCreateJavaFileStub(
                decompiledKotlinFile: JetFile,
                virtualFile: VirtualFile): PsiJavaFileStubImpl? {
            val cachedJavaStub = decompiledKotlinFile.getUserData(cachedJavaStubKey)
            val fileModificationStamp = virtualFile.modificationStamp
            if (cachedJavaStub != null && cachedJavaStub.modificationStamp == fileModificationStamp) {
                return cachedJavaStub.javaFileStub
            }
            val stub = createStub(virtualFile) as PsiJavaFileStubImpl?
            if (stub != null) {
                decompiledKotlinFile.putUserData(cachedJavaStubKey, CachedJavaStub(fileModificationStamp, stub))
            }
            return stub
        }

        private fun createStub(file: VirtualFile): PsiJavaFileStub? {
            if (file.fileType !== JavaClassFileType.INSTANCE) return null

            try {
                return ClsFileImpl.buildFileStub(file, file.contentsToByteArray())
            }
            catch (e: ClsFormatException) {
                LOG.debug(e)
            }
            catch (e: IOException) {
                LOG.debug(e)
            }

            LOG.error("Failed to build java cls class for " + file.canonicalPath!!)
            return null
        }
    }
}
