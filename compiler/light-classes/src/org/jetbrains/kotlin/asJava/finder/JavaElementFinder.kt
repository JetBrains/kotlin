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

package org.jetbrains.kotlin.asJava.finder

import com.google.common.collect.Collections2
import com.google.common.collect.Sets
import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Condition
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiUtilCore
import com.intellij.util.SmartList
import com.intellij.util.containers.ContainerUtil
import org.jetbrains.kotlin.asJava.KotlinAsJavaSupport
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.isValidJavaFqName
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.jvm.KotlinFinderMarker
import java.util.*

class JavaElementFinder(
    private val project: Project,
    private val kotlinAsJavaSupport: KotlinAsJavaSupport
) : PsiElementFinder(), KotlinFinderMarker {
    private val psiManager: PsiManager

    init {
        this.psiManager = PsiManager.getInstance(project)
    }

    override fun findClass(qualifiedName: String, scope: GlobalSearchScope): PsiClass? {
        val allClasses = findClasses(qualifiedName, scope)
        return if (allClasses.size > 0) allClasses[0] else null
    }

    override fun findClasses(qualifiedNameString: String, scope: GlobalSearchScope): Array<PsiClass> {
        if (!isValidJavaFqName(qualifiedNameString)) {
            return PsiClass.EMPTY_ARRAY
        }

        val answer = SmartList<PsiClass>()

        val qualifiedName = FqName(qualifiedNameString)

        findClassesAndObjects(qualifiedName, scope, answer)
        answer.addAll(kotlinAsJavaSupport.getFacadeClasses(qualifiedName, scope))
        answer.addAll(kotlinAsJavaSupport.getKotlinInternalClasses(qualifiedName, scope))

        return sortByClasspath(answer, scope).toTypedArray()
    }

    // Finds explicitly declared classes and objects, not package classes
    // Also DefaultImpls classes of interfaces
    private fun findClassesAndObjects(qualifiedName: FqName, scope: GlobalSearchScope, answer: MutableList<PsiClass>) {
        findInterfaceDefaultImpls(qualifiedName, scope, answer)

        val classOrObjectDeclarations = kotlinAsJavaSupport.findClassOrObjectDeclarations(qualifiedName, scope)

        for (declaration in classOrObjectDeclarations) {
            if (declaration !is KtEnumEntry) {
                val lightClass = declaration.toLightClass()
                if (lightClass != null) {
                    answer.add(lightClass)
                }
            }
        }
    }

    private fun findInterfaceDefaultImpls(qualifiedName: FqName, scope: GlobalSearchScope, answer: MutableList<PsiClass>) {
        if (qualifiedName.isRoot) return

        if (qualifiedName.shortName().asString() != JvmAbi.DEFAULT_IMPLS_CLASS_NAME) return

        for (classOrObject in kotlinAsJavaSupport.findClassOrObjectDeclarations(qualifiedName.parent(), scope)) {
            //NOTE: can't filter out more interfaces right away because decompiled declarations do not have member bodies
            if (classOrObject is KtClass && classOrObject.isInterface()) {
                val interfaceClass = classOrObject.toLightClass()
                if (interfaceClass != null) {
                    val implsClass = interfaceClass!!.findInnerClassByName(JvmAbi.DEFAULT_IMPLS_CLASS_NAME, false)
                    if (implsClass != null) {
                        answer.add(implsClass)
                    }
                }
            }
        }
    }

    override fun getClassNames(psiPackage: PsiPackage, scope: GlobalSearchScope): Set<String> {
        val packageFQN = FqName(psiPackage.qualifiedName)

        val declarations = kotlinAsJavaSupport.findClassOrObjectDeclarationsInPackage(packageFQN, scope)

        val answer = Sets.newHashSet<String>()
        answer.addAll(kotlinAsJavaSupport.getFacadeNames(packageFQN, scope))

        for (declaration in declarations) {
            val name = declaration.name
            if (name != null) {
                answer.add(name)
            }
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
        val packageFQN = FqName(psiPackage.qualifiedName)

        val subpackages = kotlinAsJavaSupport.getSubPackages(packageFQN, scope)

        val answer = Collections2.transform<FqName, PsiPackage>(subpackages) { input -> KtLightPackage(psiManager, input, scope) }

        return answer.toTypedArray()
    }

    override fun getClasses(psiPackage: PsiPackage, scope: GlobalSearchScope): Array<PsiClass> {
        val answer = SmartList<PsiClass>()
        val packageFQN = FqName(psiPackage.qualifiedName)

        answer.addAll(kotlinAsJavaSupport.getFacadeClassesInPackage(packageFQN, scope))

        val declarations = kotlinAsJavaSupport.findClassOrObjectDeclarationsInPackage(packageFQN, scope)
        for (declaration in declarations) {
            val aClass = declaration.toLightClass()
            if (aClass != null) {
                answer.add(aClass)
            }
        }

        return sortByClasspath(answer, scope).toTypedArray()
    }

    override fun getPackageFiles(psiPackage: PsiPackage, scope: GlobalSearchScope): Array<PsiFile> {
        val packageFQN = FqName(psiPackage.qualifiedName)
        // TODO: this does not take into account JvmPackageName annotation
        val result = kotlinAsJavaSupport.findFilesForPackage(packageFQN, scope)
        return result.toTypedArray()
    }

    override fun getPackageFilesFilter(psiPackage: PsiPackage, scope: GlobalSearchScope): Condition<PsiFile>? {
        return Condition { input ->
            if (input !is KtFile) {
                true
            }
            else {
                psiPackage.qualifiedName == (input as KtFile).packageFqName.asString()
            }
        }
    }

    companion object {

        fun getInstance(project: Project): JavaElementFinder {
            val extensions = Extensions.getArea(project).getExtensionPoint(PsiElementFinder.EP_NAME).extensions
            for (extension in extensions) {
                if (extension is JavaElementFinder) {
                    return extension
                }
            }
            throw IllegalStateException(JavaElementFinder::class.java.simpleName + " is not found for project " + project)
        }

        fun byClasspathComparator(searchScope: GlobalSearchScope): Comparator<PsiElement> {
            return Comparator { o1, o2 ->
                val f1 = PsiUtilCore.getVirtualFile(o1)
                val f2 = PsiUtilCore.getVirtualFile(o2)
                when {
                    f1 === f2 -> 0
                    f1 == null -> -1
                    f2 == null -> 1
                    else -> searchScope.compare(f2!!, f1!!)
                }
            }
        }

        private fun sortByClasspath(classes: List<PsiClass>, searchScope: GlobalSearchScope): Collection<PsiClass> {
            if (classes.size > 1) {
                ContainerUtil.quickSort(classes, byClasspathComparator(searchScope))
            }

            return classes
        }
    }
}
