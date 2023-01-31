/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.decompiled.light.classes.DecompiledLightClassesFactory
import org.jetbrains.kotlin.analysis.decompiled.light.classes.KtLightClassForDecompiledDeclaration
import org.jetbrains.kotlin.analysis.decompiler.psi.file.KtClsFile
import org.jetbrains.kotlin.analysis.project.structure.*
import org.jetbrains.kotlin.analysis.providers.createAllLibrariesModificationTracker
import org.jetbrains.kotlin.analysis.providers.createDeclarationProvider
import org.jetbrains.kotlin.analysis.providers.createPackageProvider
import org.jetbrains.kotlin.analysis.providers.createProjectWideOutOfBlockModificationTracker
import org.jetbrains.kotlin.asJava.KotlinAsJavaSupportBase
import org.jetbrains.kotlin.asJava.classes.KtFakeLightClass
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.asJava.classes.KtLightClassForFacade
import org.jetbrains.kotlin.light.classes.symbol.classes.SymbolBasedFakeLightClass
import org.jetbrains.kotlin.light.classes.symbol.classes.SymbolLightClassForFacade
import org.jetbrains.kotlin.light.classes.symbol.classes.createSymbolLightClassNoCache
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.parentOrNull
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtScript

class SymbolKotlinAsJavaSupport(project: Project) : KotlinAsJavaSupportBase<KtModule>(project) {
    override fun findClassOrObjectDeclarationsInPackage(
        packageFqName: FqName,
        searchScope: GlobalSearchScope
    ): Collection<KtClassOrObject> = project.createDeclarationProvider(searchScope).run {
        getTopLevelKotlinClassLikeDeclarationNamesInPackage(packageFqName).flatMap {
            getAllClassesByClassId(ClassId.topLevel(packageFqName.child(it)))
        }
    }

    override fun findFilesForPackage(packageFqName: FqName, searchScope: GlobalSearchScope): Collection<KtFile> = buildSet {
        addAll(project.createDeclarationProvider(searchScope).findFilesForFacadeByPackage(packageFqName))
        findClassOrObjectDeclarationsInPackage(packageFqName, searchScope).mapTo(this) {
            it.containingKtFile
        }
    }

    override fun findFilesForFacadeByPackage(packageFqName: FqName, searchScope: GlobalSearchScope): Collection<KtFile> {
        return project.createDeclarationProvider(searchScope).findFilesForFacadeByPackage(packageFqName)
    }

    private fun FqName.toClassIdSequence(): Sequence<ClassId> {
        var currentName = shortNameOrSpecial()
        if (currentName.isSpecial) return emptySequence()
        var currentParent = parentOrNull() ?: return emptySequence()
        var currentRelativeName = currentName.asString()

        return sequence {
            while (true) {
                yield(ClassId(currentParent, FqName(currentRelativeName), false))
                currentName = currentParent.shortNameOrSpecial()
                if (currentName.isSpecial) break
                currentParent = currentParent.parentOrNull() ?: break
                currentRelativeName = "${currentName.asString()}.$currentRelativeName"
            }
        }
    }

    override fun findClassOrObjectDeclarations(fqName: FqName, searchScope: GlobalSearchScope): Collection<KtClassOrObject> =
        fqName.toClassIdSequence().flatMap {
            project.createDeclarationProvider(searchScope).getAllClassesByClassId(it)
        }
            .filter { it.isFromSourceOrLibraryBinary(project) }
            .toSet()

    override fun packageExists(fqName: FqName, scope: GlobalSearchScope): Boolean =
        project.createPackageProvider(scope).doKotlinPackageExists(fqName)

    override fun getSubPackages(fqn: FqName, scope: GlobalSearchScope): Collection<FqName> =
        project.createPackageProvider(scope)
            .getKotlinSubPackageFqNames(fqn)
            .map { fqn.child(it) }

    override fun createInstanceOfLightScript(script: KtScript): KtLightClass? = error("Should not be called")

    override fun KtFile.findModule(): KtModule = getKtModule(project)

    override fun declarationLocation(file: KtFile): DeclarationLocation? = when (file.getKtModule(project)) {
        is KtSourceModule -> DeclarationLocation.ProjectSources
        is KtLibraryModule -> DeclarationLocation.LibraryClasses
        is KtLibrarySourceModule -> DeclarationLocation.LibrarySources
        else -> null
    }

    override fun createInstanceOfDecompiledLightClass(classOrObject: KtClassOrObject): KtLightClass? {
        return DecompiledLightClassesFactory.getLightClassForDecompiledClassOrObject(classOrObject, project)
    }

    override fun createInstanceOfLightClass(classOrObject: KtClassOrObject): KtLightClass? {
        return createSymbolLightClassNoCache(classOrObject, classOrObject.getKtModule(project))
    }

    override fun createInstanceOfDecompiledLightFacade(facadeFqName: FqName, files: List<KtFile>): KtLightClassForFacade? {
        return DecompiledLightClassesFactory.createLightFacadeForDecompiledKotlinFile(project, facadeFqName, files)
    }

    override fun projectWideOutOfBlockModificationTracker(): ModificationTracker {
        return project.createProjectWideOutOfBlockModificationTracker()
    }

    override fun outOfBlockModificationTracker(element: PsiElement): ModificationTracker {
        return project.createProjectWideOutOfBlockModificationTracker()
    }

    override fun librariesTracker(element: PsiElement): ModificationTracker {
        return project.createAllLibrariesModificationTracker()
    }

    override fun createInstanceOfLightFacade(
        facadeFqName: FqName,
        files: List<KtFile>,
    ): KtLightClassForFacade = SymbolLightClassForFacade(facadeFqName, files, files.first().getKtModule(project))

    override val KtModule.contentSearchScope: GlobalSearchScope get() = this.contentScope

    override fun facadeIsApplicable(module: KtModule, file: KtFile): Boolean = module.isFromSourceOrLibraryBinary()

    override fun getScriptClasses(scriptFqName: FqName, scope: GlobalSearchScope): Collection<PsiClass> = error("Should not be called")

    override fun getKotlinInternalClasses(fqName: FqName, scope: GlobalSearchScope): Collection<PsiClass> {
        val facadeKtFiles = project.createDeclarationProvider(scope).findInternalFilesForFacade(fqName)
        if (facadeKtFiles.isEmpty()) return emptyList()

        val partShortName = fqName.shortName().asString()
        val partClassFileShortName = "$partShortName.class"

        return facadeKtFiles.mapNotNull { facadeKtFile ->
            if (facadeKtFile is KtClsFile) {
                val partClassFile = facadeKtFile.virtualFile.parent.findChild(partClassFileShortName) ?: return@mapNotNull null
                val psiFile = facadeKtFile.manager.findFile(partClassFile) as? KtClsFile ?: facadeKtFile
                val javaClsClass = DecompiledLightClassesFactory.createClsJavaClassFromVirtualFile(
                    mirrorFile = psiFile,
                    classFile = partClassFile,
                    correspondingClassOrObject = null,
                    project = project,
                ) ?: return@mapNotNull null

                KtLightClassForDecompiledDeclaration(javaClsClass, javaClsClass.parent, psiFile, null)
            } else {
                null
            }
        }
    }

    override fun findFilesForFacade(facadeFqName: FqName, searchScope: GlobalSearchScope): Collection<KtFile> {
        return project.createDeclarationProvider(searchScope).findFilesForFacade(facadeFqName)
    }

    override fun getFakeLightClass(classOrObject: KtClassOrObject): KtFakeLightClass = SymbolBasedFakeLightClass(classOrObject)

    private fun KtElement.isFromSourceOrLibraryBinary(project: Project): Boolean = getKtModule(project).isFromSourceOrLibraryBinary()

    private fun KtModule.isFromSourceOrLibraryBinary() = when (this) {
        is KtSourceModule -> true
        is KtLibraryModule -> true
        else -> false
    }
}
