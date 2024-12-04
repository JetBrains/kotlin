/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.finder

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Condition
import com.intellij.psi.*
import com.intellij.psi.impl.compiled.ClsClassImpl
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiUtilCore
import com.intellij.util.SmartList
import com.intellij.util.containers.ContainerUtil
import org.jetbrains.kotlin.asJava.KotlinAsJavaSupport
import org.jetbrains.kotlin.asJava.hasRepeatableAnnotationContainer
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.isValidJavaFqName
import org.jetbrains.kotlin.progress.ProgressIndicatorAndCompilationCanceledStatus
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.jvm.KotlinFinderMarker
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull

class JavaElementFinder(
    private val project: Project,
) : PsiElementFinder(), KotlinFinderMarker {
    private val psiManager = PsiManager.getInstance(project)
    private val kotlinAsJavaSupport = KotlinAsJavaSupport.getInstance(project)

    override fun findClass(qualifiedName: String, scope: GlobalSearchScope) = findClasses(qualifiedName, scope).firstOrNull()

    override fun findClasses(qualifiedNameString: String, scope: GlobalSearchScope): Array<PsiClass> {
        if (!isValidJavaFqName(qualifiedNameString)) {
            return PsiClass.EMPTY_ARRAY
        }

        val answer = SmartList<PsiClass>()

        val qualifiedName = FqName(qualifiedNameString)

        findClassesAndObjects(qualifiedName, scope, answer)
        answer.addAll(kotlinAsJavaSupport.getFacadeClasses(qualifiedName, scope))
        answer.addAll(kotlinAsJavaSupport.getKotlinInternalClasses(qualifiedName, scope))

        sortByPreferenceToSourceFile(answer, scope)

        return answer.toTypedArray()
    }

    // Finds explicitly declared classes and objects, not package classes
    // Also DefaultImpls classes of interfaces, Container classes of repeatable annotations
    private fun findClassesAndObjects(qualifiedName: FqName, scope: GlobalSearchScope, answer: MutableList<PsiClass>) {
        findInterfaceDefaultImpls(qualifiedName, scope, answer)
        findRepeatableAnnotationContainer(qualifiedName, scope, answer)

        val classOrObjectDeclarations = kotlinAsJavaSupport.findClassOrObjectDeclarations(qualifiedName, scope)

        for (declaration in classOrObjectDeclarations) {
            if (declaration !is KtEnumEntry) {
                val lightClass = kotlinAsJavaSupport.getLightClass(declaration)
                if (lightClass != null) {
                    answer.add(lightClass)
                }
            }
        }
    }

    private fun findInterfaceDefaultImpls(qualifiedName: FqName, scope: GlobalSearchScope, answer: MutableList<PsiClass>) =
        findSyntheticInnerClass(qualifiedName, JvmAbi.DEFAULT_IMPLS_CLASS_NAME, scope, answer) {
            it is KtClass && it.isInterface()
        }

    private fun findRepeatableAnnotationContainer(qualifiedName: FqName, scope: GlobalSearchScope, answer: MutableList<PsiClass>) =
        findSyntheticInnerClass(qualifiedName, JvmAbi.REPEATABLE_ANNOTATION_CONTAINER_NAME, scope, answer) {
            it.hasRepeatableAnnotationContainer
        }

    private fun findSyntheticInnerClass(
        qualifiedName: FqName,
        syntheticName: String,
        scope: GlobalSearchScope,
        answer: MutableList<PsiClass>,
        predicate: (KtClassOrObject) -> Boolean,
    ) {
        if (qualifiedName.isRoot || qualifiedName.shortName().asString() != syntheticName) return

        for (classOrObject in kotlinAsJavaSupport.findClassOrObjectDeclarations(qualifiedName.parent(), scope)) {
            ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()
            if (predicate(classOrObject)) {
                val interfaceClass = kotlinAsJavaSupport.getLightClass(classOrObject) ?: continue
                val implsClass = interfaceClass.findInnerClassByName(syntheticName, false) ?: continue
                answer.add(implsClass)
            }
        }
    }

    override fun getClassNames(psiPackage: PsiPackage, scope: GlobalSearchScope): Set<String> {
        val packageFQN = FqName(psiPackage.qualifiedName)

        val declarations = kotlinAsJavaSupport.findClassOrObjectDeclarationsInPackage(packageFQN, scope)

        val answer = hashSetOf<String>()
        answer.addAll(kotlinAsJavaSupport.getFacadeNames(packageFQN, scope))

        for (declaration in declarations) {
            val name = declaration.name ?: continue
            answer.add(name)
        }

        return answer
    }

    override fun findPackage(qualifiedNameString: String): PsiPackage? {
        if (!isValidJavaFqName(qualifiedNameString)) {
            return null
        }

        val fqName = FqName(qualifiedNameString)

        // allScope() because the contract says that the whole project
        val allScope = GlobalSearchScope.allScope(project)
        return if (kotlinAsJavaSupport.packageExists(fqName, allScope)) {
            KtLightPackage(psiManager, fqName, allScope)
        } else null

    }

    override fun getSubPackages(psiPackage: PsiPackage, scope: GlobalSearchScope): Array<PsiPackage> {
        val subpackages = kotlinAsJavaSupport.getSubPackages(FqName(psiPackage.qualifiedName), scope)
        return subpackages.map { KtLightPackage(psiManager, it, scope) }.toTypedArray()
    }

    override fun getClasses(psiPackage: PsiPackage, scope: GlobalSearchScope): Array<PsiClass> {
        val answer = SmartList<PsiClass>()
        val packageFQN = FqName(psiPackage.qualifiedName)

        answer.addAll(kotlinAsJavaSupport.getFacadeClassesInPackage(packageFQN, scope))

        val declarations = kotlinAsJavaSupport.findClassOrObjectDeclarationsInPackage(packageFQN, scope)
        for (declaration in declarations) {
            val aClass = kotlinAsJavaSupport.getLightClass(declaration) ?: continue
            answer.add(aClass)
        }

        sortByPreferenceToSourceFile(answer, scope)

        return answer.toTypedArray()
    }

    private fun sortByPreferenceToSourceFile(list: SmartList<PsiClass>, searchScope: GlobalSearchScope) {
        if (list.size < 2) return
        // NOTE: this comparator might violate the contract depending on the scope passed
        ContainerUtil.quickSort(list, byClasspathComparator(searchScope))
        list.sortBy { it !is ClsClassImpl }
    }

    // TODO: this does not take into account JvmPackageName annotation
    override fun getPackageFiles(psiPackage: PsiPackage, scope: GlobalSearchScope): Array<PsiFile> =
        kotlinAsJavaSupport.findFilesForPackage(FqName(psiPackage.qualifiedName), scope).toTypedArray()

    override fun getPackageFilesFilter(psiPackage: PsiPackage, scope: GlobalSearchScope): Condition<PsiFile> = Condition { input ->
        if (input !is KtFile) {
            true
        } else {
            psiPackage.qualifiedName == input.packageFqName.asString()
        }
    }

    companion object {
        fun getInstance(project: Project): JavaElementFinder =
            EP.getPoint(project).extensions.firstIsInstanceOrNull()
                ?: error(JavaElementFinder::class.java.simpleName + " is not found for project " + project)

        fun byClasspathComparator(searchScope: GlobalSearchScope): Comparator<PsiElement> = Comparator { o1, o2 ->
            val f1 = PsiUtilCore.getVirtualFile(o1)
            val f2 = PsiUtilCore.getVirtualFile(o2)
            when {
                f1 === f2 -> 0
                f1 == null -> -1
                f2 == null -> 1
                else -> searchScope.compare(f2, f1)
            }
        }
    }
}
