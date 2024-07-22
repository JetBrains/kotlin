/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.classes

import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.*
import com.intellij.psi.impl.PsiClassImplUtil
import com.intellij.psi.impl.PsiImplUtil
import com.intellij.psi.impl.light.*
import com.intellij.psi.util.PsiUtil
import com.intellij.util.ArrayUtil
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import org.jetbrains.kotlin.utils.addIfNotNull
import java.util.*

class KotlinClassInnerStuffCache(
    private val myClass: KtExtensibleLightClass,
    private val dependencies: List<Any>,
    private val lazyCreator: LazyCreator,
    private val generateEnumMethods: Boolean = true,
) {
    abstract class LazyCreator {
        abstract fun <T : Any> get(initializer: () -> T, dependencies: List<Any>): Lazy<T>
    }

    private fun <T : Any> cache(initializer: () -> T): Lazy<T> = lazyCreator.get(initializer, dependencies)

    private val constructorsCache = cache { PsiImplUtil.getConstructors(myClass) }

    val constructors: Array<PsiMethod>
        get() = copy(constructorsCache.value)

    private val fieldsCache = cache {
        val own = myClass.ownFields
        val ext = collectAugments(myClass, PsiField::class.java)
        ArrayUtil.mergeCollections(own, ext, PsiField.ARRAY_FACTORY)
    }

    val fields: Array<PsiField>
        get() = copy(fieldsCache.value)

    private val methodsCache = cache {
        val own = myClass.ownMethods
        var ext = collectAugments(myClass, PsiMethod::class.java)
        if (generateEnumMethods && myClass.isEnum) {
            ext = ArrayList<PsiMethod>(ext.size + 2).also {
                it += ext
                it.addIfNotNull(getValuesMethod())
                it.addIfNotNull(getValueOfMethod())
            }
        }

        ArrayUtil.mergeCollections(own, ext, PsiMethod.ARRAY_FACTORY)
    }

    val methods: Array<PsiMethod>
        get() = copy(methodsCache.value)

    private val innerClassesCache = cache {
        val own = myClass.ownInnerClasses
        val ext = collectAugments(myClass, PsiClass::class.java)
        ArrayUtil.mergeCollections(own, ext, PsiClass.ARRAY_FACTORY)
    }

    val innerClasses: Array<out PsiClass>
        get() = copy(innerClassesCache.value)

    private val fieldByNameCache = cache {
        val fields = this.fields.takeIf { it.isNotEmpty() } ?: return@cache emptyMap()
        Collections.unmodifiableMap(Object2ObjectOpenHashMap<String, PsiField>(fields.size).apply {
            for (field in fields) {
                putIfAbsent(field.name, field)
            }
        })
    }

    fun findFieldByName(name: String, checkBases: Boolean): PsiField? {
        return if (checkBases) {
            PsiClassImplUtil.findFieldByName(myClass, name, true)
        } else {
            fieldByNameCache.value[name]
        }
    }

    private val methodByNameCache = cache {
        val methods = this.methods.takeIf { it.isNotEmpty() } ?: return@cache emptyMap()
        Collections.unmodifiableMap(Object2ObjectOpenHashMap<String, Array<PsiMethod>>().apply {
            for ((key, list) in methods.groupByTo(HashMap()) { it.name }) {
                put(key, list.toTypedArray())
            }
        })
    }

    fun findMethodsByName(name: String, checkBases: Boolean): Array<PsiMethod> {
        return if (checkBases) {
            PsiClassImplUtil.findMethodsByName(myClass, name, true)
        } else {
            copy(methodByNameCache.value[name] ?: PsiMethod.EMPTY_ARRAY)
        }
    }

    private val innerClassByNameCache = cache {
        val classes = this.innerClasses.takeIf { it.isNotEmpty() } ?: return@cache emptyMap()

        Collections.unmodifiableMap(Object2ObjectOpenHashMap<String, PsiClass>().apply {
            for (psiClass in classes) {
                val name = psiClass.name
                if (name == null) {
                    Logger.getInstance(KotlinClassInnerStuffCache::class.java).error("$psiClass has no name")
                } else if (psiClass !is ExternallyDefinedPsiElement || !containsKey(name)) {
                    put(name, psiClass)
                }
            }
        })
    }

    fun findInnerClassByName(name: String, checkBases: Boolean): PsiClass? {
        return if (checkBases) {
            PsiClassImplUtil.findInnerByName(myClass, name, true)
        } else {
            innerClassByNameCache.value[name]
        }
    }

    private val valuesMethodCache = cache { getEnumValuesPsiMethod(myClass) }

    private fun getValuesMethod(): PsiMethod? {
        if (myClass.isEnum && !myClass.isAnonymous && !isClassNameSealed()) {
            return valuesMethodCache.value
        }

        return null
    }

    private val valueOfMethodCache = cache { getEnumValueOfPsiMethod(myClass) }

    fun getValueOfMethod(): PsiMethod? {
        if (myClass.isEnum && !myClass.isAnonymous) {
            return valueOfMethodCache.value
        }

        return null
    }

    private fun isClassNameSealed(): Boolean {
        return myClass.name == PsiKeyword.SEALED && PsiUtil.getLanguageLevel(myClass).toJavaVersion().feature >= 16
    }
}

private val PsiClass.isAnonymous: Boolean
    get() = name == null || this is PsiAnonymousClass

private fun <T> copy(value: Array<T>): Array<T> {
    return if (value.isEmpty()) value else value.clone()
}
