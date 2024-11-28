// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
@file:JvmName("KotlinUserDataUtils")
package org.jetbrains.kotlin.analysis.api.dumdum.stubindex

import com.intellij.openapi.util.Key
import com.intellij.openapi.util.UserDataHolder
import com.intellij.psi.PsiElement
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

private class UserDataCachedDelegate<in T : UserDataHolder, out V>(
    key: String,
    private val modificationStampFactory: (T) -> Long,
    private val valueFactory: (T) -> V
): ReadOnlyProperty<T, V> {
    private val key = Key<ValueHolder<V>>(key)

    private class ValueHolder<V>(val value: V, val modificationStamp: Long)

    override fun getValue(thisRef: T, property: KProperty<*>): V {
        val cached = thisRef.getUserData(key)
        val modificationStamp = modificationStampFactory(thisRef)
        if (cached != null && modificationStamp == cached.modificationStamp) {
            return cached.value
        }

        val value = valueFactory(thisRef)
        thisRef.putUserData(key, ValueHolder(value, modificationStamp))
        return value
    }
}

fun <T : UserDataHolder, V> userDataCached(key: String, modificationStampFactory: (T) -> Long, valueFactory: (T) -> V): ReadOnlyProperty<T, V> {
    return UserDataCachedDelegate(key, modificationStampFactory, valueFactory)
}

fun <T : PsiElement, V> userDataCached(key: String, valueFactory: (T) -> V): ReadOnlyProperty<T, V> {
    return userDataCached(key, { it.containingFile.modificationStamp }, valueFactory)
}