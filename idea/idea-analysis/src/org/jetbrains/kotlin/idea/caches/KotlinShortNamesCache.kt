/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.caches

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiShortNamesCache
import com.intellij.psi.stubs.StubIndex
import com.intellij.util.ArrayUtil
import com.intellij.util.Processor
import com.intellij.util.Processors
import com.intellij.util.containers.ContainerUtil
import com.intellij.util.indexing.IdFilter
import gnu.trove.THashSet
import org.jetbrains.kotlin.asJava.LightClassUtil
import org.jetbrains.kotlin.asJava.defaultImplsChild
import org.jetbrains.kotlin.asJava.finder.JavaElementFinder
import org.jetbrains.kotlin.asJava.getAccessorLightMethods
import org.jetbrains.kotlin.fileClasses.javaFileFacadeFqName
import org.jetbrains.kotlin.idea.decompiler.builtIns.KotlinBuiltInFileType
import org.jetbrains.kotlin.idea.stubindex.*
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.load.java.getPropertyNamesCandidatesByAccessorName
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtNamedFunction

class KotlinShortNamesCache(private val project: Project) : PsiShortNamesCache() {
    companion object {
        private val LOG = Logger.getInstance(KotlinShortNamesCache::class.java)
    }

    //hacky way to avoid searches for Kotlin classes, when looking for Java (from Kotlin)
    val disableSearch: ThreadLocal<Boolean> = object : ThreadLocal<Boolean>() {
        override fun initialValue(): Boolean = false
    }

    //region Classes

    override fun processAllClassNames(processor: StringProcessor): Boolean {
        if (disableSearch.get()) return true
        return KotlinClassShortNameIndex.getInstance().processAllKeys(project, processor) &&
                KotlinFileFacadeShortNameIndex.INSTANCE.processAllKeys(project, processor)
    }

    override fun processAllClassNames(processor: StringProcessor, scope: GlobalSearchScope, filter: IdFilter?): Boolean {
        if (disableSearch.get()) return true
        return processAllClassNames(processor)
    }

    /**
     * Return kotlin class names from project sources which should be visible from java.
     */
    override fun getAllClassNames(): Array<String> {
        if (disableSearch.get()) return ArrayUtil.EMPTY_STRING_ARRAY
        return withArrayProcessor(ArrayUtil.EMPTY_STRING_ARRAY) { processor ->
            processAllClassNames(processor)
        }
    }

    override fun processClassesWithName(
        name: String,
        processor: Processor<in PsiClass>,
        scope: GlobalSearchScope,
        filter: IdFilter?
    ): Boolean {
        if (disableSearch.get()) return true
        val effectiveScope = kotlinDeclarationsVisibleFromJavaScope(scope)
        val fqNameProcessor = Processor<FqName> { fqName: FqName? ->
            if (fqName == null) return@Processor true

            val isInterfaceDefaultImpl = name == JvmAbi.DEFAULT_IMPLS_CLASS_NAME && fqName.shortName().asString() != name

            if (fqName.shortName().asString() != name && !isInterfaceDefaultImpl) {
                LOG.error(
                    "A declaration obtained from index has non-matching name:" +
                            "\nin index: $name" +
                            "\ndeclared: ${fqName.shortName()}($fqName)"
                )

                return@Processor true
            }

            val fqNameToSearch = if (isInterfaceDefaultImpl) fqName.defaultImplsChild() else fqName

            val psiClass = JavaElementFinder.getInstance(project).findClass(fqNameToSearch.asString(), effectiveScope)
                ?: return@Processor true

            return@Processor processor.process(psiClass)
        }

        val allKtClassOrObjectsProcessed = StubIndex.getInstance().processElements(
            KotlinClassShortNameIndex.getInstance().key,
            name,
            project,
            effectiveScope,
            filter,
            KtClassOrObject::class.java
        ) { ktClassOrObject ->
            fqNameProcessor.process(ktClassOrObject.fqName)
        }
        if (!allKtClassOrObjectsProcessed) {
            return false
        }

        return StubIndex.getInstance().processElements(
            KotlinFileFacadeShortNameIndex.getInstance().key,
            name,
            project,
            effectiveScope,
            filter,
            KtFile::class.java
        ) { ktFile ->
            fqNameProcessor.process(ktFile.javaFileFacadeFqName)
        }
    }

    /**
     * Return class names form kotlin sources in given scope which should be visible as Java classes.
     */
    override fun getClassesByName(name: String, scope: GlobalSearchScope): Array<PsiClass> {
        if (disableSearch.get()) return PsiClass.EMPTY_ARRAY
        return withArrayProcessor(PsiClass.EMPTY_ARRAY) { processor ->
            processClassesWithName(name, processor, scope, null)
        }
    }

    private fun kotlinDeclarationsVisibleFromJavaScope(scope: GlobalSearchScope): GlobalSearchScope {
        val noBuiltInsScope: GlobalSearchScope = object : GlobalSearchScope(project) {
            override fun isSearchInModuleContent(aModule: Module) = true
            override fun compare(file1: VirtualFile, file2: VirtualFile) = 0
            override fun isSearchInLibraries() = true
            override fun contains(file: VirtualFile) = file.fileType != KotlinBuiltInFileType
        }
        return KotlinSourceFilterScope.sourceAndClassFiles(scope, project).intersectWith(noBuiltInsScope)
    }
    //endregion

    //region Methods

    override fun processAllMethodNames(processor: StringProcessor, scope: GlobalSearchScope, filter: IdFilter?): Boolean {
        if (disableSearch.get()) return true
        return processAllMethodNames(processor)
    }

    override fun getAllMethodNames(): Array<String> {
        if (disableSearch.get()) ArrayUtil.EMPTY_STRING_ARRAY
        return withArrayProcessor(ArrayUtil.EMPTY_STRING_ARRAY) { processor ->
            processAllMethodNames(processor)
        }
    }

    private fun processAllMethodNames(processor: StringProcessor): Boolean {
        if (disableSearch.get()) return true
        if (!KotlinFunctionShortNameIndex.getInstance().processAllKeys(project, processor)) {
            return false
        }

        return KotlinPropertyShortNameIndex.getInstance().processAllKeys(project) { name ->
            return@processAllKeys processor.process(JvmAbi.setterName(name)) && processor.process(JvmAbi.getterName(name))
        }
    }

    override fun processMethodsWithName(
        name: String,
        processor: Processor<in PsiMethod>,
        scope: GlobalSearchScope,
        filter: IdFilter?
    ): Boolean {
        if (disableSearch.get()) return true
        val allFunctionsProcessed = StubIndex.getInstance().processElements(
            KotlinFunctionShortNameIndex.getInstance().key,
            name,
            project,
            scope,
            filter,
            KtNamedFunction::class.java
        ) { ktNamedFunction ->
            val methods = LightClassUtil.getLightClassMethods(ktNamedFunction).filter { it.name == name }
            return@processElements methods.all { method ->
                processor.process(method)
            }
        }
        if (!allFunctionsProcessed) {
            return false
        }

        for (propertyName in getPropertyNamesCandidatesByAccessorName(Name.identifier(name))) {
            val allProcessed = StubIndex.getInstance().processElements(
                KotlinPropertyShortNameIndex.getInstance().key,
                propertyName.asString(),
                project,
                scope,
                filter,
                KtNamedDeclaration::class.java
            ) { ktNamedDeclaration ->
                val methods: Sequence<PsiMethod> = ktNamedDeclaration.getAccessorLightMethods()
                    .asSequence()
                    .filter { it.name == name }

                return@processElements methods.all { method ->
                    processor.process(method)
                }
            }
            if (!allProcessed) {
                return false
            }
        }

        return true
    }

    override fun getMethodsByName(name: String, scope: GlobalSearchScope): Array<PsiMethod> {
        if (disableSearch.get()) return PsiMethod.EMPTY_ARRAY
        return withArrayProcessor(PsiMethod.EMPTY_ARRAY) { processor ->
            processMethodsWithName(name, processor, scope, null)
        }
    }

    override fun getMethodsByNameIfNotMoreThan(name: String, scope: GlobalSearchScope, maxCount: Int): Array<PsiMethod> {
        if (disableSearch.get()) return PsiMethod.EMPTY_ARRAY
        require(maxCount >= 0)

        return withArrayProcessor(PsiMethod.EMPTY_ARRAY) { processor ->
            processMethodsWithName(
                name,
                { psiMethod ->
                    processor.size != maxCount && processor.process(psiMethod)
                },
                scope,
                null
            )
        }
    }

    override fun processMethodsWithName(name: String, scope: GlobalSearchScope, processor: Processor<PsiMethod>): Boolean {
        if (disableSearch.get()) return true
        return ContainerUtil.process(getMethodsByName(name, scope), processor)
    }
    //endregion

    //region Fields

    override fun processAllFieldNames(processor: StringProcessor, scope: GlobalSearchScope, filter: IdFilter?): Boolean {
        if (disableSearch.get()) return true
        return processAllFieldNames(processor)
    }

    override fun getAllFieldNames(): Array<String> {
        if (disableSearch.get()) return ArrayUtil.EMPTY_STRING_ARRAY
        return withArrayProcessor(ArrayUtil.EMPTY_STRING_ARRAY) { processor ->
            processAllFieldNames(processor)
        }
    }

    private fun processAllFieldNames(processor: StringProcessor): Boolean {
        if (disableSearch.get()) return true
        return KotlinPropertyShortNameIndex.getInstance().processAllKeys(project, processor)
    }

    override fun processFieldsWithName(
        name: String,
        processor: Processor<in PsiField>,
        scope: GlobalSearchScope,
        filter: IdFilter?
    ): Boolean {
        if (disableSearch.get()) return true
        return StubIndex.getInstance().processElements(
            KotlinPropertyShortNameIndex.getInstance().key,
            name,
            project,
            scope,
            filter,
            KtNamedDeclaration::class.java
        ) { ktNamedDeclaration ->
            val field = LightClassUtil.getLightClassBackingField(ktNamedDeclaration)
                ?: return@processElements true

            return@processElements processor.process(field)
        }
    }

    override fun getFieldsByName(name: String, scope: GlobalSearchScope): Array<PsiField> {
        if (disableSearch.get()) return PsiField.EMPTY_ARRAY
        return withArrayProcessor(PsiField.EMPTY_ARRAY) { processor ->
            processFieldsWithName(name, processor, scope, null)
        }
    }

    override fun getFieldsByNameIfNotMoreThan(name: String, scope: GlobalSearchScope, maxCount: Int): Array<PsiField> {
        if (disableSearch.get()) return PsiField.EMPTY_ARRAY
        require(maxCount >= 0)

        return withArrayProcessor(PsiField.EMPTY_ARRAY) { processor ->
            processFieldsWithName(
                name,
                { psiField ->
                    processor.size != maxCount && processor.process(psiField)
                },
                scope,
                null
            )
        }
    }
    //endregion

    private inline fun <T> withArrayProcessor(
        result: Array<T>,
        process: (CancelableArrayCollectProcessor<T>) -> Unit
    ): Array<T> {
        return CancelableArrayCollectProcessor<T>().also { processor ->
            process(processor)
        }.toArray(result)
    }

    private class CancelableArrayCollectProcessor<T> : Processor<T> {
        val troveSet = THashSet<T>()
        private val processor = Processors.cancelableCollectProcessor<T>(troveSet)

        override fun process(value: T): Boolean {
            return processor.process(value)
        }

        val size: Int get() = troveSet.size

        fun toArray(a: Array<T>): Array<T> = troveSet.toArray(a)
    }
}
