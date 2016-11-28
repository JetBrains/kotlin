/*
 * Copyright 2010-2016 JetBrains s.r.o.
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
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.ClassFileViewProvider
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.compiled.ClsClassImpl
import com.intellij.psi.impl.compiled.ClsFileImpl
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.asJava.LightClassGenerationSupport
import org.jetbrains.kotlin.asJava.builder.ClsWrapperStubPsiFactory
import org.jetbrains.kotlin.asJava.builder.LightClassConstructionContext
import org.jetbrains.kotlin.asJava.classes.FakeLightClassForFileOfPackage
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.asJava.classes.KtLightClassForFacade
import org.jetbrains.kotlin.asJava.classes.KtLightClassForSourceDeclaration
import org.jetbrains.kotlin.asJava.finder.JavaElementFinder
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.fileClasses.JvmFileClassUtil
import org.jetbrains.kotlin.fileClasses.javaFileFacadeFqName
import org.jetbrains.kotlin.idea.decompiler.classFile.KtClsFile
import org.jetbrains.kotlin.idea.decompiler.navigation.SourceNavigationHelper
import org.jetbrains.kotlin.idea.project.ResolveElementCache
import org.jetbrains.kotlin.idea.stubindex.*
import org.jetbrains.kotlin.idea.stubindex.KotlinSourceFilterScope.Companion.sourceAndClassFiles
import org.jetbrains.kotlin.idea.util.ProjectRootsUtil
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.load.kotlin.PackagePartClassUtils
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getElementTextWithContext
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.lazy.ForceResolveUtil
import org.jetbrains.kotlin.resolve.lazy.NoDescriptorForDeclarationException
import org.jetbrains.kotlin.resolve.lazy.ResolveSession
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.utils.sure
import java.util.*

class IDELightClassGenerationSupport(private val project: Project) : LightClassGenerationSupport() {
    private val scopeFileComparator = JavaElementFinder.byClasspathComparator(GlobalSearchScope.allScope(project))
    private val psiManager: PsiManager = PsiManager.getInstance(project)

    override fun getContextForClassOrObject(classOrObject: KtClassOrObject): LightClassConstructionContext {
        if (classOrObject.isLocal()) {
            return getContextForLocalClassOrObject(classOrObject)
        }
        else {
            return getContextForNonLocalClassOrObject(classOrObject)
        }
    }

    private fun getContextForNonLocalClassOrObject(classOrObject: KtClassOrObject): LightClassConstructionContext {
        val resolutionFacade = classOrObject.getResolutionFacade()
        val bindingContext = if (classOrObject is KtClass && classOrObject.isAnnotation()) {
            // need to make sure default values for parameters are resolved
            // because java resolve depends on whether there is a default value for an annotation attribute
            resolutionFacade.getFrontendService(ResolveElementCache::class.java)
                    .resolvePrimaryConstructorParametersDefaultValues(classOrObject)
        }
        else {
            resolutionFacade.analyze(classOrObject)
        }
        val classDescriptor = bindingContext.get(BindingContext.CLASS, classOrObject).sure {
            "Class descriptor was not found for ${classOrObject.getElementTextWithContext()}"
        }
        ForceResolveUtil.forceResolveAllContents(classDescriptor)
        return LightClassConstructionContext(bindingContext, resolutionFacade.moduleDescriptor)
    }

    private fun getContextForLocalClassOrObject(classOrObject: KtClassOrObject): LightClassConstructionContext {
        val resolutionFacade = classOrObject.getResolutionFacade()
        val bindingContext = resolutionFacade.analyze(classOrObject)

        val descriptor = bindingContext.get(BindingContext.CLASS, classOrObject)

        if (descriptor == null) {
            LOG.warn("No class descriptor in context for class: " + classOrObject.getElementTextWithContext())
            return LightClassConstructionContext(bindingContext, resolutionFacade.moduleDescriptor)
        }

        ForceResolveUtil.forceResolveAllContents<ClassDescriptor>(descriptor)

        return LightClassConstructionContext(bindingContext, resolutionFacade.moduleDescriptor)
    }

    override fun getContextForFacade(files: Collection<KtFile>): LightClassConstructionContext {
        assert(!files.isEmpty()) { "No files in facade" }

        val sortedFiles = files.sortedWith(scopeFileComparator)
        val file = sortedFiles.first()
        val resolveSession = file.getResolutionFacade().getFrontendService(ResolveSession::class.java)
        forceResolvePackageDeclarations(files, resolveSession)
        return LightClassConstructionContext(resolveSession.bindingContext, resolveSession.moduleDescriptor)
    }

    override fun findClassOrObjectDeclarations(fqName: FqName, searchScope: GlobalSearchScope): Collection<KtClassOrObject> {
        return KotlinFullClassNameIndex.getInstance().get(fqName.asString(), project, sourceAndClassFiles(searchScope, project))
    }

    override fun findFilesForPackage(fqName: FqName, searchScope: GlobalSearchScope): Collection<KtFile> {
        return PackageIndexUtil.findFilesWithExactPackage(fqName, sourceAndClassFiles(searchScope, project), project)
    }

    override fun findClassOrObjectDeclarationsInPackage(
            packageFqName: FqName,
            searchScope: GlobalSearchScope
    ): Collection<KtClassOrObject> {
        return KotlinTopLevelClassByPackageIndex.getInstance().get(
                packageFqName.asString(), project, sourceAndClassFiles(searchScope, project))
    }

    override fun packageExists(fqName: FqName, scope: GlobalSearchScope): Boolean {
        return PackageIndexUtil.packageExists(fqName, sourceAndClassFiles(scope, project), project)
    }

    override fun getSubPackages(fqn: FqName, scope: GlobalSearchScope): Collection<FqName> {
        return PackageIndexUtil.getSubPackageFqNames(fqn, sourceAndClassFiles(scope, project), project, MemberScope.ALL_NAME_FILTER)
    }

    override fun getLightClass(classOrObject: KtClassOrObject): KtLightClass? {
        val virtualFile = classOrObject.containingFile.virtualFile
        if (virtualFile != null) {
            when {
                ProjectRootsUtil.isProjectSourceFile(project, virtualFile) ->
                    return KtLightClassForSourceDeclaration.create(classOrObject)
                ProjectRootsUtil.isLibraryClassFile(project, virtualFile) ->
                    return getLightClassForDecompiledClassOrObject(classOrObject)
                ProjectRootsUtil.isLibrarySourceFile(project, virtualFile) ->
                    return SourceNavigationHelper.getOriginalClass(classOrObject) as? KtLightClass
            }
        }
        if ((classOrObject.containingFile as? KtFile)?.analysisContext != null) {
            // explicit request to create light class from dummy.kt
            return KtLightClassForSourceDeclaration.create(classOrObject)
        }
        return null
    }

    private fun withFakeLightClasses(
            lightClassForFacade: KtLightClassForFacade?,
            facadeFiles: List<KtFile>
    ): List<PsiClass> {
        if (lightClassForFacade == null) return emptyList()

        val lightClasses = ArrayList<PsiClass>()
        lightClasses.add(lightClassForFacade)
        if (facadeFiles.size > 1) {
            lightClasses.addAll(facadeFiles.map {
                FakeLightClassForFileOfPackage(lightClassForFacade, it)
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

    override fun getMultifilePartClasses(partFqName: FqName, scope: GlobalSearchScope): Collection<PsiClass> {
        if (partFqName.isRoot) return emptyList()

        val facadeKtFiles = StaticFacadeIndexUtil.getMultifileClassForPart(partFqName, scope, project)
        val partShortName = partFqName.shortName().asString()
        val partClassFileShortName = partShortName + ".class"

        return facadeKtFiles.mapNotNull { facadeKtFile ->
            if (facadeKtFile is KtClsFile) {
                val partClassFile = facadeKtFile.virtualFile.parent.findChild(partClassFileShortName) ?: return@mapNotNull null
                val javaClsClass = createClsJavaClassFromVirtualFile(facadeKtFile, partClassFile, null) ?: return@mapNotNull null
                KtLightClassForDecompiledDeclaration(javaClsClass, null, facadeKtFile)
            }
            else {
                // TODO should we build light classes for parts from source?
                null
            }
        }
    }

    fun createLightClassForFileFacade(
            facadeFqName: FqName,
            facadeFiles: List<KtFile>,
            moduleInfo: IdeaModuleInfo
    ): List<PsiClass> {
        if (moduleInfo is ModuleSourceInfo) {
            val lightClassForFacade = KtLightClassForFacade.createForFacade(
                    psiManager, facadeFqName, moduleInfo.contentScope(), facadeFiles)
            return withFakeLightClasses(lightClassForFacade, facadeFiles)
        }
        else {
            return facadeFiles.filterIsInstance<KtClsFile>().mapNotNull { createLightClassForDecompiledKotlinFile(it) }
        }
    }

    override fun findFilesForFacade(facadeFqName: FqName, scope: GlobalSearchScope): Collection<KtFile> {
        return KotlinFileFacadeFqNameIndex.INSTANCE.get(facadeFqName.asString(), project, scope)
    }

    override fun resolveToDescriptor(declaration: KtDeclaration): DeclarationDescriptor? {
        try {
            return declaration.resolveToDescriptor()
        }
        catch (e: NoDescriptorForDeclarationException) {
            return null
        }
    }

    override fun analyze(element: KtElement) = element.analyze(BodyResolveMode.PARTIAL)

    override fun getFacadeNames(packageFqName: FqName, scope: GlobalSearchScope): Collection<String> {
        val facadeFilesInPackage = KotlinFileFacadeClassByPackageIndex.getInstance().get(packageFqName.asString(), project, scope)
        return facadeFilesInPackage.map { it.javaFileFacadeFqName.shortName().asString() }
    }

    override fun getFacadeClassesInPackage(packageFqName: FqName, scope: GlobalSearchScope): Collection<PsiClass> {
        val facadeFilesInPackage = KotlinFileFacadeClassByPackageIndex.getInstance().get(packageFqName.asString(), project, scope)
        val groupedByFqNameAndModuleInfo = facadeFilesInPackage.groupBy {
            Pair(it.javaFileFacadeFqName, it.getModuleInfo())
        }

        return groupedByFqNameAndModuleInfo.flatMap {
            val (key, files) = it
            val (fqName, moduleInfo) = key
            createLightClassForFileFacade(fqName, files, moduleInfo)
        }
    }

    private fun forceResolvePackageDeclarations(files: Collection<KtFile>, session: ResolveSession) {
        for (file in files) {
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
                if (declaration is KtFunction) {
                    val name = declaration.nameAsSafeName
                    val functions = packageDescriptor.memberScope.getContributedFunctions(name, NoLookupLocation.FROM_IDE)
                    for (descriptor in functions) {
                        ForceResolveUtil.forceResolveAllContents(descriptor)
                    }
                }
                else if (declaration is KtProperty) {
                    val name = declaration.nameAsSafeName
                    val properties = packageDescriptor.memberScope.getContributedVariables(name, NoLookupLocation.FROM_IDE)
                    for (descriptor in properties) {
                        ForceResolveUtil.forceResolveAllContents(descriptor)
                    }
                }
                else if (declaration is KtClassOrObject || declaration is KtTypeAlias || declaration is KtDestructuringDeclaration) {
                    // Do nothing: we are not interested in classes or type aliases,
                    // and all destructuring declarations are erroneous at top level
                }
                else {
                    LOG.error("Unsupported declaration kind: " + declaration + " in file " + file.name + "\n" + file.text)
                }
            }

            ForceResolveUtil.forceResolveAllContents(session.getFileAnnotations(file))
        }
    }

    private fun getLightClassForDecompiledClassOrObject(decompiledClassOrObject: KtClassOrObject): KtLightClassForDecompiledDeclaration? {
        if (decompiledClassOrObject is KtEnumEntry) {
            return null
        }
        val containingKtFile = decompiledClassOrObject.containingFile as? KtClsFile ?: return null
        val rootLightClassForDecompiledFile = createLightClassForDecompiledKotlinFile(containingKtFile) ?: return null

        return findCorrespondingLightClass(decompiledClassOrObject, rootLightClassForDecompiledFile)
    }

    private fun findCorrespondingLightClass(
            decompiledClassOrObject: KtClassOrObject,
            rootLightClassForDecompiledFile: KtLightClassForDecompiledDeclaration
    ): KtLightClassForDecompiledDeclaration {
        val relativeFqName = getClassRelativeName(decompiledClassOrObject)
        val iterator = relativeFqName.pathSegments().iterator()
        val base = iterator.next()
        assert(rootLightClassForDecompiledFile.name == base.asString()) { "Light class for file:\n" + decompiledClassOrObject.getContainingKtFile().virtualFile.canonicalPath + "\nwas expected to have name: " + base.asString() + "\n Actual: " + rootLightClassForDecompiledFile.name }
        var current: KtLightClassForDecompiledDeclaration = rootLightClassForDecompiledFile
        while (iterator.hasNext()) {
            val name = iterator.next()
            val innerClass = current.findInnerClassByName(name.asString(), false).sure {
                "Could not find corresponding inner/nested class " + relativeFqName + " in class " + decompiledClassOrObject.fqName + "\n" + "File: " + decompiledClassOrObject.getContainingKtFile().virtualFile.name
            }
            current = innerClass as KtLightClassForDecompiledDeclaration
        }
        return current
    }

    private fun getClassRelativeName(decompiledClassOrObject: KtClassOrObject): FqName {
        val name = decompiledClassOrObject.nameAsName!!
        val parent = PsiTreeUtil.getParentOfType(decompiledClassOrObject, KtClassOrObject::class.java, true)
        if (parent == null) {
            assert(decompiledClassOrObject.isTopLevel())
            return FqName.topLevel(name)
        }
        return getClassRelativeName(parent).child(name)
    }

    fun createLightClassForDecompiledKotlinFile(file: KtClsFile): KtLightClassForDecompiledDeclaration? {
        val virtualFile = file.virtualFile ?: return null

        val classOrObject = file.declarations.filterIsInstance<KtClassOrObject>().singleOrNull()

        val javaClsClass = createClsJavaClassFromVirtualFile(
                file, virtualFile,
                correspondingClassOrObject = classOrObject
        ) ?: return null

        return KtLightClassForDecompiledDeclaration(javaClsClass, classOrObject, file)
    }

    private fun createClsJavaClassFromVirtualFile(
            mirrorFile: KtFile,
            classFile: VirtualFile,
            correspondingClassOrObject: KtClassOrObject?
    ): ClsClassImpl? {
        val javaFileStub = ClsJavaStubByVirtualFileCache.getInstance(project).get(classFile) ?: return null
        javaFileStub.psiFactory = ClsWrapperStubPsiFactory.INSTANCE
        val manager = PsiManager.getInstance(mirrorFile.project)
        val fakeFile = object : ClsFileImpl(ClassFileViewProvider(manager, classFile)) {
            override fun getNavigationElement(): PsiElement {
                if (correspondingClassOrObject != null) {
                    return correspondingClassOrObject.navigationElement.containingFile
                }
                return super.getNavigationElement()
            }

            override fun getStub() = javaFileStub

            override fun getMirror() = mirrorFile

            override fun isPhysical() = false
        }
        javaFileStub.psi = fakeFile
        return fakeFile.classes.single() as ClsClassImpl
    }

    companion object {
        private val LOG = Logger.getInstance(IDELightClassGenerationSupport::class.java)
    }
}

class KtFileClassProviderImpl(val lightClassGenerationSupport: LightClassGenerationSupport) : KtFileClassProvider {
    override fun getFileClasses(file: KtFile): Array<PsiClass> {
        if (file.isCompiled) {
            return arrayOf()
        }

        val result = arrayListOf<PsiClass>()
        file.declarations.filterIsInstance<KtClassOrObject>().map { lightClassGenerationSupport.getLightClass(it) }.filterNotNullTo(result)

        val moduleInfo = file.getModuleInfo()
        val jvmClassInfo = JvmFileClassUtil.getFileClassInfoNoResolve(file)
        val fileClassFqName = file.javaFileFacadeFqName

        val facadeClasses = when {
            file.analysisContext != null && PackagePartClassUtils.fileHasTopLevelCallables(file) ->
                listOf(KtLightClassForFacade.createForSyntheticFile(PsiManager.getInstance(file.project), fileClassFqName, file))

            jvmClassInfo.withJvmMultifileClass ->
                lightClassGenerationSupport.getFacadeClasses(fileClassFqName, moduleInfo.contentScope())

            PackagePartClassUtils.fileHasTopLevelCallables(file) ->
                (lightClassGenerationSupport as IDELightClassGenerationSupport).createLightClassForFileFacade(
                        fileClassFqName, listOf(file), moduleInfo)

            else -> emptyList<PsiClass>()
        }

        facadeClasses.filterTo(result) {
            it is KtLightClassForFacade && file in it.files
        }

        return result.toTypedArray()
    }
}
