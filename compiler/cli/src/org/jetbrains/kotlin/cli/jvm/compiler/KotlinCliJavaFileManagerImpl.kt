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
import com.intellij.psi.*
import com.intellij.psi.impl.file.PsiPackageImpl
import com.intellij.psi.search.GlobalSearchScope
import gnu.trove.THashMap
import org.jetbrains.kotlin.cli.jvm.index.JavaRoot
import org.jetbrains.kotlin.cli.jvm.index.JvmDependenciesIndex
import org.jetbrains.kotlin.cli.jvm.index.SingleJavaFileRootsIndex
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.impl.JavaClassImpl
import org.jetbrains.kotlin.load.java.structure.impl.classFiles.BinaryClassSignatureParser
import org.jetbrains.kotlin.load.java.structure.impl.classFiles.BinaryJavaClass
import org.jetbrains.kotlin.load.java.structure.impl.classFiles.ClassifierResolutionContext
import org.jetbrains.kotlin.load.java.structure.impl.classFiles.isNotTopLevelClass
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.jvm.KotlinCliJavaFileManager
import org.jetbrains.kotlin.util.PerformanceCounter
import org.jetbrains.kotlin.utils.addIfNotNull
import java.util.*

// TODO: do not inherit from CoreJavaFileManager to avoid accidental usage of its methods which do not use caches/indices
// Currently, the only relevant usage of this class as CoreJavaFileManager is at CoreJavaDirectoryService.getPackage,
// which is indirectly invoked from PsiPackage.getSubPackages
class KotlinCliJavaFileManagerImpl(private val myPsiManager: PsiManager) : CoreJavaFileManager(myPsiManager), KotlinCliJavaFileManager {
    private val perfCounter = PerformanceCounter.create("Find Java class")
    private lateinit var index: JvmDependenciesIndex
    private lateinit var singleJavaFileRootsIndex: SingleJavaFileRootsIndex
    private val topLevelClassesCache: MutableMap<FqName, VirtualFile?> = THashMap()
    private val allScope = GlobalSearchScope.allScope(myPsiManager.project)
    private var useFastClassFilesReading = false

    fun initialize(index: JvmDependenciesIndex, singleJavaFileRootsIndex: SingleJavaFileRootsIndex, useFastClassFilesReading: Boolean) {
        this.index = index
        this.singleJavaFileRootsIndex = singleJavaFileRootsIndex
        this.useFastClassFilesReading = useFastClassFilesReading
    }

    private fun findPsiClass(classId: ClassId, searchScope: GlobalSearchScope): PsiClass? = perfCounter.time {
        findVirtualFileForTopLevelClass(classId, searchScope)?.findPsiClassInVirtualFile(classId.relativeClassName.asString())
    }

    private fun findVirtualFileForTopLevelClass(classId: ClassId, searchScope: GlobalSearchScope): VirtualFile? {
        val relativeClassName = classId.relativeClassName.asString()
        return topLevelClassesCache.getOrPut(classId.packageFqName.child(classId.relativeClassName.pathSegments().first())) {
            index.findClass(classId) { dir, type ->
                findVirtualFileGivenPackage(dir, relativeClassName, type)
            }
            ?: singleJavaFileRootsIndex.findJavaSourceClass(classId)
        }?.takeIf { it in searchScope }
    }

    private val binaryCache: MutableMap<ClassId, JavaClass?> = THashMap()
    private val signatureParsingComponent =
            BinaryClassSignatureParser(ClassifierResolutionContext { findClass(it, allScope) })

    override fun findClass(classId: ClassId, searchScope: GlobalSearchScope): JavaClass? {
        val virtualFile = findVirtualFileForTopLevelClass(classId, searchScope) ?: return null

        if (useFastClassFilesReading && virtualFile.extension == "class") {
            // We return all class files' names in the directory in knownClassNamesInPackage method, so one may request an inner class
            return binaryCache.getOrPut(classId) {
                // Note that currently we implicitly suppose that searchScope for binary classes is constant and we do not use it
                // as a key in cache
                // This is a true assumption by now since there are two search scopes in compiler: one for sources and another one for binary
                // When it become wrong because we introduce the modules into CLI, it's worth to consider
                // having different KotlinCliJavaFileManagerImpl's for different modules
                val classContent = virtualFile.contentsToByteArray()
                if (virtualFile.nameWithoutExtension.contains("$") && isNotTopLevelClass(classContent)) return@getOrPut null
                classId.outerClassId?.let { outerClassId ->
                    val outerClass = findClass(outerClassId, searchScope)
                    return@getOrPut outerClass?.findInnerClass(classId.shortClassName)
                }

                val resolver = ClassifierResolutionContext { findClass(it, allScope) }

                BinaryJavaClass(
                        virtualFile,
                        classId.asSingleFqName(),
                        resolver,
                        signatureParsingComponent,
                        outerClass = null,
                        classContent = classContent
                )
            }
        }

        return virtualFile.findPsiClassInVirtualFile(classId.relativeClassName.asString())?.let(::JavaClassImpl)
    }

    // this method is called from IDEA to resolve dependencies in Java code
    // which supposedly shouldn't have errors so the dependencies exist in general
    override fun findClass(qName: String, scope: GlobalSearchScope): PsiClass? {
        // String cannot be reliably converted to ClassId because we don't know where the package name ends and class names begin.
        // For example, if qName is "a.b.c.d.e", we should either look for a top level class "e" in the package "a.b.c.d",
        // or, for example, for a nested class with the relative qualified name "c.d.e" in the package "a.b".
        // Below, we start by looking for the top level class "e" in the package "a.b.c.d" first, then for the class "d.e" in the package
        // "a.b.c", and so on, until we find something. Most classes are top level, so most of the times the search ends quickly

        forEachClassId(qName) { classId ->
            findPsiClass(classId, scope)?.let { return it }
        }

        return null
    }

    private inline fun forEachClassId(fqName: String, block: (ClassId) -> Unit) {
        var classId = fqName.toSafeTopLevelClassId() ?: return

        while (true) {
            block(classId)

            val packageFqName = classId.packageFqName
            if (packageFqName.isRoot) break

            classId = ClassId(
                    packageFqName.parent(),
                    FqName(packageFqName.shortName().asString() + "." + classId.relativeClassName.asString()),
                    false
            )
        }
    }

    override fun findClasses(qName: String, scope: GlobalSearchScope): Array<PsiClass> = perfCounter.time {
        val result = ArrayList<PsiClass>(1)
        forEachClassId(qName) { classId ->
            val relativeClassName = classId.relativeClassName.asString()
            index.traverseDirectoriesInPackage(classId.packageFqName) { dir, rootType ->
                val psiClass =
                        findVirtualFileGivenPackage(dir, relativeClassName, rootType)
                            ?.takeIf { it in scope }
                            ?.findPsiClassInVirtualFile(relativeClassName)
                if (psiClass != null) {
                    result.add(psiClass)
                }
                // traverse all
                true
            }

            result.addIfNotNull(
                    singleJavaFileRootsIndex.findJavaSourceClass(classId)
                            ?.takeIf { it in scope }
                            ?.findPsiClassInVirtualFile(relativeClassName)
            )

            if (result.isNotEmpty()) {
                return@time result.toTypedArray()
            }
        }

        PsiClass.EMPTY_ARRAY
    }

    override fun findPackage(packageName: String): PsiPackage? {
        var found = false
        val packageFqName = packageName.toSafeFqName() ?: return null
        index.traverseDirectoriesInPackage(packageFqName) { _, _ ->
            found = true
            //abort on first found
            false
        }
        if (!found) {
            found = singleJavaFileRootsIndex.findJavaSourceClasses(packageFqName).isNotEmpty()
        }
        return if (found) PsiPackageImpl(myPsiManager, packageName) else null
    }

    private fun findVirtualFileGivenPackage(
            packageDir: VirtualFile,
            classNameWithInnerClasses: String,
            rootType: JavaRoot.RootType
    ): VirtualFile? {
        val topLevelClassName = classNameWithInnerClasses.substringBefore('.')

        val vFile = when (rootType) {
            JavaRoot.RootType.BINARY -> packageDir.findChild("$topLevelClassName.class")
            JavaRoot.RootType.SOURCE -> packageDir.findChild("$topLevelClassName.java")
        } ?: return null

        if (!vFile.isValid) {
            LOG.error("Invalid child of valid parent: ${vFile.path}; ${packageDir.isValid} path=${packageDir.path}")
            return null
        }

        return vFile
    }

    private fun VirtualFile.findPsiClassInVirtualFile(
            classNameWithInnerClasses: String
    ): PsiClass? {
        val file = myPsiManager.findFile(this) as? PsiClassOwner ?: return null
        return findClassInPsiFile(classNameWithInnerClasses, file)
    }

    override fun knownClassNamesInPackage(packageFqName: FqName): Set<String> {
        val result = hashSetOf<String>()
        index.traverseDirectoriesInPackage(packageFqName, continueSearch = {
            dir, _ ->

            for (child in dir.children) {
                if (child.extension == "class" || child.extension == "java") {
                    result.add(child.nameWithoutExtension)
                }
            }

            true
        })

        for (classId in singleJavaFileRootsIndex.findJavaSourceClasses(packageFqName)) {
            assert(!classId.isNestedClass) { "ClassId of a single .java source class should not be nested: $classId" }
            result.add(classId.shortClassName.asString())
        }

        return result
    }

    override fun getNonTrivialPackagePrefixes(): Collection<String> = emptyList()

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
                val innerClass = curClass.findInnerClassByName(innerClassName, false) ?: return null
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
