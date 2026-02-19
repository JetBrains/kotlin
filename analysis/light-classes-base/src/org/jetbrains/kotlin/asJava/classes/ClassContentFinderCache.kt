/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.asJava.classes

import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.util.ModificationTracker
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import com.intellij.psi.impl.PsiClassImplUtil
import com.intellij.psi.impl.source.PsiExtensibleClass
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import org.jetbrains.kotlin.utils.exceptions.logErrorWithAttachment
import org.jetbrains.kotlin.utils.exceptions.withPsiEntry

/**
 * Inspired by [ClassInnerStuffCache][com.intellij.psi.impl.source.ClassInnerStuffCache].
 *
 * This class contains only caches for `find*ByName` methods.
 *
 * @see com.intellij.psi.impl.source.ClassInnerStuffCache
 */
class ClassContentFinderCache(
    private val extensibleClass: PsiExtensibleClass,
    private val modificationTrackers: List<ModificationTracker>,
) {
    fun findFieldByName(name: String, checkBases: Boolean): PsiField? = if (checkBases) {
        PsiClassImplUtil.findFieldByName(/* aClass = */ extensibleClass, /* name = */ name, /* checkBases = */ true)
    } else {
        val cachedMap = cachedMap(PsiExtensibleClass::fieldsMap)
        cachedMap[name]
    }

    fun findMethodsByName(name: String, checkBases: Boolean): Array<PsiMethod> = if (checkBases) {
        PsiClassImplUtil.findMethodsByName(/* aClass = */ extensibleClass, /* name = */ name, /* checkBases = */ true)
    } else {
        val cachedMap = cachedMap(PsiExtensibleClass::methodsMap)
        val methods = cachedMap[name]
        methods?.takeUnless { it.isEmpty() }?.toTypedArray() ?: PsiMethod.EMPTY_ARRAY
    }

    fun findInnerClassByName(name: String, checkBases: Boolean): PsiClass? = if (checkBases) {
        PsiClassImplUtil.findInnerByName(/* aClass = */ extensibleClass, /* name = */ name, /* checkBases = */ true)
    } else {
        val cachedMap = cachedMap(PsiExtensibleClass::innerClassesMap)
        cachedMap[name]
    }

    private inline fun <T> cachedMap(
        crossinline provider: (PsiExtensibleClass) -> Map<String, T>,
    ): Map<String, T> = CachedValuesManager.getCachedValue(
        extensibleClass,
        CachedValueProvider {
            CachedValueProvider.Result.create(
                provider(extensibleClass),
                // Theoretically, this modification tracker should be calculated based on `extensibleClass`
                // or should be used as a key for the cache map.
                // But practically, `modificationTrackers` are bundled into the specific `extensibleClass`,
                // so we save the invariant here.
                // Hense, it is impossible to access the cache with the same `extensibleClass`,
                // but with another `modificationTrackers`.
                modificationTrackers,
            )
        }
    )
}

private val PsiExtensibleClass.fieldsMap: Map<String, PsiField>
    get() = buildMap {
        for (field in ownFields) {
            val name = field.name
            putIfAbsent(name, field)
        }
    }

private val PsiExtensibleClass.methodsMap: Map<String, List<PsiMethod>>
    get() = ownMethods.groupBy { it.name }.ifEmpty { emptyMap() }

private val PsiExtensibleClass.innerClassesMap: Map<String, PsiClass>
    get() = buildMap {
        for (innerClass in ownInnerClasses) {
            val name = innerClass.name
            if (name == null) {
                thisLogger().logErrorWithAttachment("Inner class doesn't have a name") {
                    withPsiEntry("innerClass", innerClass)
                }
            } else {
                putIfAbsent(name, innerClass)
            }
        }
    }
