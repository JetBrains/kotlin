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

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.libraries.LibraryUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.ClassFileViewProvider
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.compiled.ClsClassImpl
import com.intellij.psi.impl.compiled.ClsFileImpl
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.PsiClassHolderFileStub
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.asJava.*
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.fileClasses.javaFileFacadeFqName
import org.jetbrains.kotlin.idea.decompiler.navigation.JetSourceNavigationHelper
import org.jetbrains.kotlin.idea.stubindex.*
import org.jetbrains.kotlin.idea.stubindex.JetSourceFilterScope.kotlinSourceAndClassFiles
import org.jetbrains.kotlin.idea.util.ProjectRootsUtil
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.load.kotlin.PackageClassUtils
import org.jetbrains.kotlin.load.kotlin.PackagePartClassUtils
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getElementTextWithContext
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.lazy.ForceResolveUtil
import org.jetbrains.kotlin.resolve.lazy.NoDescriptorForDeclarationException
import org.jetbrains.kotlin.resolve.lazy.ResolveSession
import org.jetbrains.kotlin.resolve.scopes.JetScope
import org.jetbrains.kotlin.utils.sure
import java.util.*

public class IDELightClassGenerationSupport(private val project: Project) : LightClassGenerationSupport() {
    private val scopeFileComparator = JavaElementFinder.byClasspathComparator(GlobalSearchScope.allScope(project))
    private val psiManager: PsiManager = PsiManager.getInstance(project)

    override fun getContextForPackage(files: Collection<JetFile>): LightClassConstructionContext {
        assert(!files.isEmpty()) { "No files in package" }

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
        assert(!files.isEmpty()) { "No files in facade" }

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
        val filesWithCallables = PackagePartClassUtils.getFilesWithCallables(findFilesForPackage(packageFqName, scope))
        val filesByModule = filesWithCallables.groupBy { it.getModuleInfo() }
        return filesByModule.flatMap {
            createLightClassForPackageFacade(it.value, it.key, packageFqName)
        }
    }

    private fun createLightClassForPackageFacade(
            files: List<JetFile>,
            moduleInfo: IdeaModuleInfo,
            packageFqName: FqName
    ): List<PsiClass> {
        if (moduleInfo is ModuleSourceInfo) {
            val lightClassForFacade = KotlinLightClassForFacade.createForPackageFacade(
                    psiManager, packageFqName, moduleInfo.contentScope(), files
            )
            return withFakeLightClasses(lightClassForFacade, files)

        }
        else {
            val packageClassName = PackageClassUtils.getPackageClassName(packageFqName)
            val virtualFileForPackageClass = files.asSequence().map {
                it.virtualFile?.parent?.findChild("$packageClassName.class")
            }.firstOrNull { it != null } ?: return emptyList()

            val clsClassFromPackageClass = createClsJavaClassFromVirtualFile(
                    mirrorFile = files.first(),
                    classFile = virtualFileForPackageClass,
                    correspondingClassOrObject = null
            ) ?: return emptyList()
            return listOf(KotlinLightClassForDecompiledDeclaration(clsClassFromPackageClass, null))
        }
    }

    private fun withFakeLightClasses(
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
        val filesByModule = findFilesForFacade(facadeFqName, scope).groupBy {
            it.getModuleInfo()
        }

        return filesByModule.flatMap {
            createLightClassForFileFacade(facadeFqName, it.value, it.key)
        }
    }

    public fun createLightClassForFileFacade(
            facadeFqName: FqName,
            facadeFiles: List<JetFile>,
            moduleInfo: IdeaModuleInfo
    ): List<PsiClass> {
        if (moduleInfo is ModuleSourceInfo) {
            val lightClassForFacade = KotlinLightClassForFacade.createForFacade(
                    psiManager, facadeFqName, moduleInfo.contentScope(), facadeFiles)
            return withFakeLightClasses(lightClassForFacade, facadeFiles)
        }
        else {
            return facadeFiles.filter { it.isCompiled }.map { createLightClassForDecompiledKotlinFile(it) }.filterNotNull()
        }
    }

    override fun findFilesForFacade(facadeFqName: FqName, scope: GlobalSearchScope): Collection<JetFile> {
        return JetFileFacadeFqNameIndex.INSTANCE.get(facadeFqName.asString(), project, scope)
    }

    override fun resolveClassToDescriptor(classOrObject: JetClassOrObject): ClassDescriptor? {
        try {
            return classOrObject.resolveToDescriptor() as ClassDescriptor
        }
        catch (e: NoDescriptorForDeclarationException) {
            return null
        }
    }

    override fun getFacadeNames(packageFqName: FqName, scope: GlobalSearchScope): Collection<String> {
        val facadeFilesInPackage = JetFileFacadeClassByPackageIndex.getInstance().get(packageFqName.asString(), project, scope)
        return facadeFilesInPackage.map { it.javaFileFacadeFqName.shortName().asString() }
    }

    override fun getFacadeClassesInPackage(packageFqName: FqName, scope: GlobalSearchScope): Collection<PsiClass> {
        val facadeFilesInPackage = JetFileFacadeClassByPackageIndex.getInstance().get(packageFqName.asString(), project, scope)
        val groupedByFqNameAndModuleInfo = facadeFilesInPackage.groupBy {
            Pair(it.javaFileFacadeFqName, it.getModuleInfo())
        }

        return groupedByFqNameAndModuleInfo.flatMap {
            val (key, files) = it
            val (fqName, moduleInfo) = key
            createLightClassForFileFacade(fqName, files, moduleInfo)
        }
    }


    private fun forceResolvePackageDeclarations(files: Collection<JetFile>, session: ResolveSession) {
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

            ForceResolveUtil.forceResolveAllContents(session.getFileAnnotations(file))
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

    private fun createLightClassForDecompiledKotlinFile(file: JetFile): KotlinLightClassForDecompiledDeclaration? {
        val virtualFile = file.virtualFile ?: return null

        val classOrObject = file.declarations.filterIsInstance<JetClassOrObject>().singleOrNull()

        val javaClsClass = createClsJavaClassFromVirtualFile(
                file, virtualFile,
                correspondingClassOrObject = classOrObject
        ) ?: return null

        return KotlinLightClassForDecompiledDeclaration(javaClsClass, classOrObject)
    }

    private fun createClsJavaClassFromVirtualFile(
            mirrorFile: JetFile,
            classFile: VirtualFile,
            correspondingClassOrObject: JetClassOrObject?
    ): ClsClassImpl? {
        val javaFileStub = ClsJavaStubByVirtualFileCache.getInstance(project).get(classFile) ?: return null
        val manager = PsiManager.getInstance(mirrorFile.project)
        val fakeFile = object : ClsFileImpl(ClassFileViewProvider(manager, classFile)) {
            override fun getNavigationElement(): PsiElement {
                if (correspondingClassOrObject != null) {
                    return correspondingClassOrObject.navigationElement.containingFile
                }
                return super.getNavigationElement()
            }

            override fun getStub(): PsiClassHolderFileStub<*> {
                return javaFileStub
            }

            override fun getMirror(): PsiElement {
                return mirrorFile
            }
        }
        fakeFile.isPhysical = false
        javaFileStub.psi = fakeFile
        return fakeFile.classes.single() as ClsClassImpl
    }

    companion object {
        private val LOG = Logger.getInstance(IDELightClassGenerationSupport::class.java)
    }
}
