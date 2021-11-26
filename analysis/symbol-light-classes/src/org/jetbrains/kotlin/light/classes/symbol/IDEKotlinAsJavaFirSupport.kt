/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.project.structure.*
import org.jetbrains.kotlin.analysis.providers.createDeclarationProvider
import org.jetbrains.kotlin.analysis.providers.createPackageProvider
import org.jetbrains.kotlin.asJava.KotlinAsJavaSupport
import org.jetbrains.kotlin.asJava.classes.KtFakeLightClass
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.fileClasses.javaFileFacadeFqName
import org.jetbrains.kotlin.light.classes.symbol.caches.SymbolLightClassFacadeCache
import org.jetbrains.kotlin.light.classes.symbol.classes.getOrCreateFirLightClass
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.parentOrNull
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty

class IDEKotlinAsJavaFirSupport(private val project: Project) : KotlinAsJavaSupport() {
    override fun findClassOrObjectDeclarationsInPackage(
        packageFqName: FqName,
        searchScope: GlobalSearchScope
    ): Collection<KtClassOrObject> = project.createDeclarationProvider(searchScope).run {
        getClassNamesInPackage(packageFqName).flatMap {
            getClassesByClassId(ClassId.topLevel(packageFqName.child(it)))
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    override fun findFilesForPackage(fqName: FqName, searchScope: GlobalSearchScope): Collection<KtFile> =
        buildSet {
            addAll(project.createDeclarationProvider(searchScope).getFacadeFilesInPackage(fqName))
            findClassOrObjectDeclarationsInPackage(fqName, searchScope).mapTo(this) {
                it.containingKtFile
            }
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
            project.createDeclarationProvider(searchScope).getClassesByClassId(it)
        }.filter {
            //TODO Do not return LC came from LibrarySources
            when (it.getKtModule(project)) {
                is KtLibrarySourceModule -> false
                is KtNotUnderContentRootModule -> false
                else -> true
            }
        }.toSet()

    override fun packageExists(fqName: FqName, scope: GlobalSearchScope): Boolean =
        project.createPackageProvider(scope).doKotlinPackageExists(fqName)

    override fun getSubPackages(fqn: FqName, scope: GlobalSearchScope): Collection<FqName> =
        project.createPackageProvider(scope)
            .getKotlinSubPackageFqNames(fqn)
            .map { fqn.child(it) }

    override fun getLightClass(classOrObject: KtClassOrObject): KtLightClass? {
        if (!classOrObject.isFromSource()) return null
        return getOrCreateFirLightClass(classOrObject)
    }

    override fun getLightClassForScript(script: KtScript): KtLightClass =
        error("Should not be called")

    override fun getFacadeClasses(facadeFqName: FqName, scope: GlobalSearchScope): Collection<PsiClass> =
        //TODO Split by modules
        findFilesForFacade(facadeFqName, scope).ifNotEmpty {
            listOfNotNull(
                project.getService(SymbolLightClassFacadeCache::class.java).getOrCreateSymbolLightFacade(this.toList(), facadeFqName)
            )
        } ?: emptyList()

    override fun getScriptClasses(scriptFqName: FqName, scope: GlobalSearchScope): Collection<PsiClass> =
        error("Should not be called")

    override fun getKotlinInternalClasses(fqName: FqName, scope: GlobalSearchScope): Collection<PsiClass> =
        emptyList() //TODO Implement if necessary for fir

    override fun getFacadeClassesInPackage(packageFqName: FqName, scope: GlobalSearchScope): Collection<PsiClass> =
        project.createDeclarationProvider(scope)
            .getFacadeFilesInPackage(packageFqName)
            .asSequence()
            .filter { it.isFromSource() }
            .groupBy { it.javaFileFacadeFqName }
            .mapNotNull {
                project.getService(SymbolLightClassFacadeCache::class.java)
                    .getOrCreateSymbolLightFacade(it.value, it.key)
            }

    override fun getFacadeNames(packageFqName: FqName, scope: GlobalSearchScope): Collection<String> =
        project.createDeclarationProvider(scope)
            .getFacadeFilesInPackage(packageFqName)
            .filter { it.isFromSource() }
            .mapTo(mutableSetOf()) { it.javaFileFacadeFqName.shortName().asString() }

    override fun findFilesForFacade(facadeFqName: FqName, scope: GlobalSearchScope): Collection<KtFile> {
        return project.createDeclarationProvider(scope)
            .findFilesForFacade(facadeFqName)
            .filter { it.isFromSource() }
    }

    override fun getFakeLightClass(classOrObject: KtClassOrObject): KtFakeLightClass =
        KtFirBasedFakeLightClass(classOrObject)

    override fun createFacadeForSyntheticFile(facadeClassFqName: FqName, file: KtFile): PsiClass =
        TODO("Not implemented")
}


private fun KtElement.isFromSource(): Boolean {
    if (this is KtFile && isCompiled) {
        // small optimisation to not invoke expensive getKtModule
        return false
    }
    return getKtModule(project) is KtSourceModule
}