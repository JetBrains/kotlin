/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.caches.resolve

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.*
import com.intellij.psi.impl.compiled.ClsClassImpl
import com.intellij.psi.impl.compiled.ClsFileImpl
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.asJava.KotlinAsJavaSupport
import org.jetbrains.kotlin.asJava.builder.ClsWrapperStubPsiFactory
import org.jetbrains.kotlin.asJava.classes.*
import org.jetbrains.kotlin.fileClasses.javaFileFacadeFqName
import org.jetbrains.kotlin.idea.caches.lightClasses.ClsJavaStubByVirtualFileCache
import org.jetbrains.kotlin.idea.caches.lightClasses.KtLightClassForDecompiledDeclaration
import org.jetbrains.kotlin.idea.caches.lightClasses.platformMutabilityWrapper
import org.jetbrains.kotlin.idea.caches.project.*
import org.jetbrains.kotlin.idea.caches.project.IdeaModuleInfo
import org.jetbrains.kotlin.idea.decompiler.classFile.KtClsFile
import org.jetbrains.kotlin.idea.decompiler.navigation.SourceNavigationHelper
import org.jetbrains.kotlin.idea.project.platform
import org.jetbrains.kotlin.idea.stubindex.*
import org.jetbrains.kotlin.idea.util.ProjectRootsUtil
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatform
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.utils.sure
import java.util.*

class IDEKotlinAsJavaSupport(private val project: Project): KotlinAsJavaSupport() {
    private val psiManager: PsiManager = PsiManager.getInstance(project)

    override fun getFacadeNames(packageFqName: FqName, scope: GlobalSearchScope): Collection<String> {
        val facadeFilesInPackage = runReadAction {
            KotlinFileFacadeClassByPackageIndex.getInstance()
                .get(packageFqName.asString(), project, scope)
        }
        return facadeFilesInPackage.map { it.javaFileFacadeFqName.shortName().asString() }
    }

    override fun getFacadeClassesInPackage(packageFqName: FqName, scope: GlobalSearchScope): Collection<PsiClass> {
        val facadeFilesInPackage = runReadAction {
            KotlinFileFacadeClassByPackageIndex.getInstance()
                .get(packageFqName.asString(), project, scope).platformSourcesFirst()
        }
        val groupedByFqNameAndModuleInfo = facadeFilesInPackage.groupBy {
            Pair(it.javaFileFacadeFqName, it.getModuleInfoPreferringJvmPlatform())
        }

        return groupedByFqNameAndModuleInfo.flatMap {
            val (key, files) = it
            val (fqName, moduleInfo) = key
            createLightClassForFileFacade(fqName, files, moduleInfo)
        }
    }

    override fun findClassOrObjectDeclarations(fqName: FqName, searchScope: GlobalSearchScope): Collection<KtClassOrObject> {
        return runReadAction {
            KotlinFullClassNameIndex.getInstance().get(
                fqName.asString(),
                project,
                KotlinSourceFilterScope.sourceAndClassFiles(searchScope, project)
            )
        }
    }

    override fun findFilesForPackage(fqName: FqName, searchScope: GlobalSearchScope): Collection<KtFile> {
        return runReadAction {
            PackageIndexUtil.findFilesWithExactPackage(
                fqName,
                KotlinSourceFilterScope.sourceAndClassFiles(
                    searchScope,
                    project
                ),
                project
            )
        }
    }

    override fun findClassOrObjectDeclarationsInPackage(
        packageFqName: FqName,
        searchScope: GlobalSearchScope
    ): Collection<KtClassOrObject> {
        return KotlinTopLevelClassByPackageIndex.getInstance().get(
            packageFqName.asString(), project,
            KotlinSourceFilterScope.sourceAndClassFiles(searchScope, project)
        )
    }

    override fun packageExists(fqName: FqName, scope: GlobalSearchScope): Boolean {
        return PackageIndexUtil.packageExists(
            fqName,
            KotlinSourceFilterScope.sourceAndClassFiles(
                scope,
                project
            ),
            project
        )
    }

    override fun getSubPackages(fqn: FqName, scope: GlobalSearchScope): Collection<FqName> {
        return PackageIndexUtil.getSubPackageFqNames(
            fqn,
            KotlinSourceFilterScope.sourceAndClassFiles(
                scope,
                project
            ),
            project,
            MemberScope.ALL_NAME_FILTER
        )
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
        if ((classOrObject.containingFile as? KtFile)?.analysisContext != null ||
            classOrObject.containingFile.originalFile.virtualFile != null
        ) {
            // explicit request to create light class from dummy.kt
            return KtLightClassForSourceDeclaration.create(classOrObject)
        }
        return null
    }

    override fun getLightClassForScript(script: KtScript): KtLightClassForScript? =
        KtLightClassForScript.create(script)

    private fun withFakeLightClasses(
        lightClassForFacade: KtLightClassForFacade,
        facadeFiles: List<KtFile>
    ): List<PsiClass> {
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
        val filesByModule = findFilesForFacade(facadeFqName, scope).groupBy(PsiElement::getModuleInfoPreferringJvmPlatform)

        return filesByModule.flatMap {
            createLightClassForFileFacade(facadeFqName, it.value, it.key)
        }
    }

    override fun getScriptClasses(scriptFqName: FqName, scope: GlobalSearchScope): Collection<PsiClass> {
        return KotlinScriptFqnIndex.instance.get(scriptFqName.asString(), project, scope).mapNotNull {
            getLightClassForScript(it)
        }
    }

    override fun getKotlinInternalClasses(fqName: FqName, scope: GlobalSearchScope): Collection<PsiClass> {
        if (fqName.isRoot) return emptyList()

        val packageParts = findPackageParts(fqName, scope)
        val platformWrapper = findPlatformWrapper(fqName, scope)
        return if (platformWrapper != null) packageParts + platformWrapper else packageParts
    }

    private fun findPackageParts(fqName: FqName, scope: GlobalSearchScope): List<KtLightClassForDecompiledDeclaration> {
        val facadeKtFiles = StaticFacadeIndexUtil.getMultifileClassForPart(fqName, scope, project)
        val partShortName = fqName.shortName().asString()
        val partClassFileShortName = partShortName + ".class"

        return facadeKtFiles.mapNotNull { facadeKtFile ->
            if (facadeKtFile is KtClsFile) {
                val partClassFile = facadeKtFile.virtualFile.parent.findChild(partClassFileShortName) ?: return@mapNotNull null
                val javaClsClass = createClsJavaClassFromVirtualFile(facadeKtFile, partClassFile, null) ?: return@mapNotNull null
                KtLightClassForDecompiledDeclaration(javaClsClass, null, facadeKtFile)
            } else {
                // TODO should we build light classes for parts from source?
                null
            }
        }
    }

    private fun findPlatformWrapper(fqName: FqName, scope: GlobalSearchScope): PsiClass? {
        return platformMutabilityWrapper(fqName) {
            JavaPsiFacade.getInstance(
                project
            ).findClass(it, scope)
        }
    }

    fun createLightClassForFileFacade(
        facadeFqName: FqName,
        facadeFiles: List<KtFile>,
        moduleInfo: IdeaModuleInfo
    ): List<PsiClass> {
        val (clsFiles, sourceFiles) = facadeFiles.partition { it is KtClsFile }
        val facadesFromCls = clsFiles.mapNotNull { createLightClassForDecompiledKotlinFile(it as KtClsFile) }
        val facadesFromSources = createFacadesForSourceFiles(moduleInfo, sourceFiles, facadeFqName)
        return facadesFromSources + facadesFromCls
    }

    private fun createFacadesForSourceFiles(
        moduleInfo: IdeaModuleInfo,
        sourceFiles: List<KtFile>,
        facadeFqName: FqName
    ): List<PsiClass> {
        if (sourceFiles.isEmpty()) return listOf()
        if (moduleInfo !is ModuleSourceInfo && moduleInfo !is PlatformModuleInfo) return listOf()

        val lightClassForFacade = KtLightClassForFacade.createForFacade(
            psiManager, facadeFqName, moduleInfo.contentScope(), sourceFiles
        )
        return withFakeLightClasses(lightClassForFacade, sourceFiles)
    }

    override fun findFilesForFacade(facadeFqName: FqName, scope: GlobalSearchScope): Collection<KtFile> {
        return runReadAction {
            KotlinFileFacadeFqNameIndex.INSTANCE.get(facadeFqName.asString(), project, scope).platformSourcesFirst()
        }
    }


    // NOTE: this is a hacky solution to the following problem:
    // when building this light class resolver will be built by the first file in the list
    // (we could assume that files are in the same module before)
    // thus we need to ensure that resolver will be built by the file from platform part of the module
    // (resolver built by a file from the common part will have no knowledge of the platform part)
    // the actual of order of files that resolver receives is controlled by *findFilesForFacade* method
    private fun Collection<KtFile>.platformSourcesFirst() = sortedByDescending { it.platform == JvmPlatform }

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
        assert(rootLightClassForDecompiledFile.name == base.asString()) { "Light class for file:\n" + decompiledClassOrObject.containingKtFile.virtualFile.canonicalPath + "\nwas expected to have name: " + base.asString() + "\n Actual: " + rootLightClassForDecompiledFile.name }
        var current: KtLightClassForDecompiledDeclaration = rootLightClassForDecompiledFile
        while (iterator.hasNext()) {
            val name = iterator.next()
            val innerClass = current.findInnerClassByName(name.asString(), false).sure {
                "Could not find corresponding inner/nested class " + relativeFqName + " in class " + decompiledClassOrObject.fqName + "\n" + "File: " + decompiledClassOrObject.containingKtFile.virtualFile.name
            }
            current = innerClass as KtLightClassForDecompiledDeclaration
        }
        return current
    }

    private fun getClassRelativeName(decompiledClassOrObject: KtClassOrObject): FqName {
        val name = decompiledClassOrObject.nameAsName!!
        val parent = PsiTreeUtil.getParentOfType(
            decompiledClassOrObject,
            KtClassOrObject::class.java,
            true
        )
        if (parent == null) {
            assert(decompiledClassOrObject.isTopLevel())
            return FqName.topLevel(name)
        }
        return getClassRelativeName(parent).child(name)
    }

    private fun createLightClassForDecompiledKotlinFile(file: KtClsFile): KtLightClassForDecompiledDeclaration? {
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
}

internal fun PsiElement.getModuleInfoPreferringJvmPlatform(): IdeaModuleInfo {
    return getPlatformModuleInfo(JvmPlatform) ?: getModuleInfo()
}