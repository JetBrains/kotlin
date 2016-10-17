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

package org.jetbrains.kotlin.cli.jvm.compiler

import com.intellij.core.CoreJavaFileManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassOwner
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiPackage
import com.intellij.psi.impl.file.PsiPackageImpl
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.cli.jvm.index.JavaRoot
import org.jetbrains.kotlin.cli.jvm.index.JvmDependenciesIndex
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.jvm.KotlinCliJavaFileManager
import org.jetbrains.kotlin.util.PerformanceCounter
import org.jetbrains.kotlin.utils.addToStdlib.check
import java.util.*
import kotlin.properties.Delegates

class KotlinCliJavaFileManagerImpl(private val myPsiManager: PsiManager) : CoreJavaFileManager(myPsiManager), KotlinCliJavaFileManager {
    private val perfCounter = PerformanceCounter.create("Find Java class")
    private var index: JvmDependenciesIndex by Delegates.notNull()
    private val allScope = GlobalSearchScope.allScope(myPsiManager.project)

    fun initIndex(packagesCache: JvmDependenciesIndex) {
        this.index = packagesCache
    }

    override fun findClass(classId: ClassId, searchScope: GlobalSearchScope): PsiClass? {
        return perfCounter.time {
            val classNameWithInnerClasses = classId.relativeClassName.asString()
            index.findClass(classId) { dir, type ->
                findClassGivenPackage(allScope, dir, classNameWithInnerClasses, type)
            }?.check { it.containingFile.virtualFile in searchScope }
        }
    }

    override fun findClass(qName: String, scope: GlobalSearchScope): PsiClass? {
        // this method is called from IDEA to resolve dependencies in Java code
        // which supposedly shouldn't have errors so the dependencies exist in general
        // Most classes are top level classes so we will try to find them fast
        // but we must sometimes fallback to support finding inner/nested classes
        return qName.toSafeTopLevelClassId()?.let { classId -> findClass(classId, scope) } ?: super.findClass(qName, scope)
    }

    override fun findClasses(qName: String, scope: GlobalSearchScope): Array<PsiClass> {
        return perfCounter.time {
            val classIdAsTopLevelClass = qName.toSafeTopLevelClassId() ?: return@time super.findClasses(qName, scope)

            val result = ArrayList<PsiClass>()
            val classNameWithInnerClasses = classIdAsTopLevelClass.relativeClassName.asString()
            index.traverseDirectoriesInPackage(classIdAsTopLevelClass.packageFqName) { dir, rootType ->
                val psiClass = findClassGivenPackage(scope, dir, classNameWithInnerClasses, rootType)
                if (psiClass != null) {
                    result.add(psiClass)
                }
                // traverse all
                true
            }
            if (result.isEmpty()) {
                super.findClasses(qName, scope)
            }
            else {
                result.toTypedArray()
            }
        }
    }

    override fun findPackage(packageName: String): PsiPackage? {
        var found = false
        val packageFqName = packageName.toSafeFqName() ?: return null
        index.traverseDirectoriesInPackage(packageFqName) { dir, rootType ->
            found = true
            //abort on first found
            false
        }
        if (found) {
            return PsiPackageImpl(myPsiManager, packageName)
        }
        return null
    }

    private fun findClassGivenPackage(
            scope: GlobalSearchScope, packageDir: VirtualFile,
            classNameWithInnerClasses: String, rootType: JavaRoot.RootType
    ): PsiClass? {
        val topLevelClassName = classNameWithInnerClasses.substringBefore('.')

        val vFile = when (rootType) {
            JavaRoot.RootType.BINARY -> packageDir.findChild("$topLevelClassName.class")
            JavaRoot.RootType.SOURCE -> packageDir.findChild("$topLevelClassName.java")
        } ?: return null

        if (!vFile.isValid) {
            LOG.error("Invalid child of valid parent: ${vFile.path}; ${packageDir.isValid} path=${packageDir.path}")
            return null
        }
        if (vFile !in scope) {
            return null
        }

        val file = myPsiManager.findFile(vFile) as? PsiClassOwner ?: return null
        return findClassInPsiFile(classNameWithInnerClasses, file)
    }

    override fun knownClassNamesInPackage(packageFqName: FqName) = index.collectKnownClassNamesInPackage(packageFqName)

    companion object {
        private val LOG = Logger.getInstance(KotlinCliJavaFileManagerImpl::class.java)

        private fun findClassInPsiFile(classNameWithInnerClassesDotSeparated: String, file: PsiClassOwner): PsiClass? {
            for (topLevelClass in file.classes) {
                val candidate = findClassByTopLevelClass(classNameWithInnerClassesDotSeparated, topLevelClass)
                if (candidate != null) {
                    return candidate
                }
            }
            return null
        }

        private fun findClassByTopLevelClass(className: String, topLevelClass: PsiClass): PsiClass? {
            if (className.indexOf('.') < 0) {
                return if (className == topLevelClass.name) topLevelClass else null
            }

            val segments = StringUtil.split(className, ".").iterator()
            if (!segments.hasNext() || segments.next() != topLevelClass.name) {
                return null
            }
            var curClass = topLevelClass
            while (segments.hasNext()) {
                val innerClassName = segments.next()
                val innerClass = curClass.findInnerClassByName(innerClassName, false)
                if (innerClass == null) {
                    return null
                }
                curClass = innerClass
            }
            return curClass
        }
    }
}

// a sad workaround to avoid throwing exception when called from inside IDEA code
private fun <T : Any> safely(compute: () -> T): T? = try {
    compute()
}
catch (e: IllegalArgumentException) {
    null
}
catch (e: AssertionError) {
    null
}

private fun String.toSafeFqName(): FqName? = safely { FqName(this) }
private fun String.toSafeTopLevelClassId(): ClassId? = safely { ClassId.topLevel(FqName(this)) }