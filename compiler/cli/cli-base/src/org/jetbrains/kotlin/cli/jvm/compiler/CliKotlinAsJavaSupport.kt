/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.compiler

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiSearchScopeUtil
import com.intellij.util.SmartList
import org.jetbrains.kotlin.asJava.KotlinAsJavaSupportBase
import org.jetbrains.kotlin.asJava.LightClassGenerationSupport
import org.jetbrains.kotlin.asJava.classes.KtDescriptorBasedFakeLightClass
import org.jetbrains.kotlin.asJava.classes.KtFakeLightClass
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.asJava.classes.KtLightClassForFacade
import org.jetbrains.kotlin.descriptors.PackageViewDescriptor
import org.jetbrains.kotlin.load.java.components.FilesByFacadeFqNameIndexer
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtScript
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.lazy.ResolveSessionUtils
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.MemberScope

class CliKotlinAsJavaSupport(project: Project, private val traceHolder: CliTraceHolder) : KotlinAsJavaSupportBase<KtFile>(project) {
    override fun findFilesForFacadeByPackage(
        packageFqName: FqName,
        searchScope: GlobalSearchScope
    ) = traceHolder.bindingContext.get(FilesByFacadeFqNameIndexer.FACADE_FILES_BY_PACKAGE_NAME, packageFqName)
        ?.filter { PsiSearchScopeUtil.isInScope(searchScope, it) }
        .orEmpty()

    override fun KtFile.findModule(): KtFile = this

    override fun createInstanceOfDecompiledLightFacade(facadeFqName: FqName, files: List<KtFile>): KtLightClassForFacade? {
        error("Should not be called")
    }

    override fun librariesTracker(element: PsiElement): ModificationTracker {
        error("Should not be called")
    }

    override fun createInstanceOfLightFacade(facadeFqName: FqName, files: List<KtFile>): KtLightClassForFacade {
        return LightClassGenerationSupport.getInstance(files.first().project).createUltraLightClassForFacade(facadeFqName, files)
    }

    override val KtFile.contentSearchScope: GlobalSearchScope get() = GlobalSearchScope.allScope(project)
    override fun facadeIsApplicable(module: KtFile, file: KtFile): Boolean = !module.isCompiled

    override fun findFilesForFacade(facadeFqName: FqName, searchScope: GlobalSearchScope): List<KtFile> {
        if (facadeFqName.isRoot) return emptyList()

        return traceHolder.bindingContext.get(FilesByFacadeFqNameIndexer.FACADE_FILES_BY_FQ_NAME, facadeFqName)?.filter {
            PsiSearchScopeUtil.isInScope(searchScope, it)
        }.orEmpty()
    }

    override fun getKotlinInternalClasses(fqName: FqName, scope: GlobalSearchScope): Collection<PsiClass> = emptyList()

    override fun getFakeLightClass(classOrObject: KtClassOrObject): KtFakeLightClass = KtDescriptorBasedFakeLightClass(classOrObject)

    override fun findClassOrObjectDeclarationsInPackage(
        packageFqName: FqName, searchScope: GlobalSearchScope
    ): Collection<KtClassOrObject> {
        val files = findFilesForPackage(packageFqName, searchScope)
        val result = SmartList<KtClassOrObject>()
        for (file in files) {
            for (declaration in file.declarations) {
                if (declaration is KtClassOrObject) {
                    result.add(declaration)
                }
            }
        }

        return result
    }

    override fun packageExists(fqName: FqName, scope: GlobalSearchScope): Boolean = !traceHolder.module.getPackage(fqName).isEmpty()

    override fun getSubPackages(fqn: FqName, scope: GlobalSearchScope): Collection<FqName> {
        val packageView = traceHolder.module.getPackage(fqn)
        return packageView.memberScope.getContributedDescriptors(
            DescriptorKindFilter.PACKAGES,
            MemberScope.ALL_NAME_FILTER
        ).mapNotNull { member -> (member as? PackageViewDescriptor)?.fqName }
    }

    override fun createInstanceOfLightScript(script: KtScript): KtLightClass {
        return LightClassGenerationSupport.getInstance(script.project).createUltraLightClassForScript(script)
    }

    override fun findClassOrObjectDeclarations(fqName: FqName, searchScope: GlobalSearchScope): Collection<KtClassOrObject> {
        return ResolveSessionUtils.getClassDescriptorsByFqName(traceHolder.module, fqName).mapNotNull {
            val element = DescriptorToSourceUtils.getSourceFromDescriptor(it)
            if (element is KtClassOrObject && PsiSearchScopeUtil.isInScope(searchScope, element)) {
                element
            } else null
        }
    }

    override fun findFilesForPackage(packageFqName: FqName, searchScope: GlobalSearchScope): Collection<KtFile> {
        return traceHolder.bindingContext.get(BindingContext.PACKAGE_TO_FILES, packageFqName)?.filter {
            PsiSearchScopeUtil.isInScope(searchScope, it)
        }.orEmpty()
    }

    override fun findFilesForScript(scriptFqName: FqName, searchScope: GlobalSearchScope): Collection<KtScript> {
        return findFilesForPackage(scriptFqName.parent(), searchScope)
            .mapNotNull { file ->
                file.script?.takeIf { it.fqName == scriptFqName }
            }
    }

    override fun createFacadeForSyntheticFile(file: KtFile): KtLightClassForFacade = error("Should not be called")
    override fun declarationLocation(file: KtFile): DeclarationLocation = DeclarationLocation.ProjectSources
    override fun createInstanceOfDecompiledLightClass(classOrObject: KtClassOrObject): KtLightClass = error("Should not be called")
    override fun createInstanceOfLightClass(classOrObject: KtClassOrObject): KtLightClass {
        return LightClassGenerationSupport.getInstance(classOrObject.project).createUltraLightClass(classOrObject)
    }
}
