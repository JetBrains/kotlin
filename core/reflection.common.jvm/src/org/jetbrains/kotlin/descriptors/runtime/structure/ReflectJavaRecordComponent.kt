/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.runtime.structure

import org.jetbrains.kotlin.load.java.structure.JavaRecordComponent
import org.jetbrains.kotlin.load.java.structure.JavaType
import java.lang.reflect.Member
import java.lang.reflect.Method

class ReflectJavaRecordComponent(val recordComponent: Any) : ReflectJavaMember(), JavaRecordComponent {
    override val type: JavaType
        get() = Java16RecordComponentsLoader.loadGetType(recordComponent)?.let { ReflectJavaClassifierType(it) }
            ?: throw NoSuchMethodError("Can't find `getType` method")
    override val isVararg: Boolean
        get() = false
    override val member: Member
        get() = Java16RecordComponentsLoader.loadGetAccessor(recordComponent)
            ?: throw NoSuchMethodError("Can't find `getAccessor` method")
}

private object Java16RecordComponentsLoader {
    class Cache(
        val getType: Method?,
        val getAccessor: Method?,
    )

    private var _cache: Cache? = null

    private fun buildCache(recordComponent: Any): Cache {
        // Should be Class<RecordComponent>
        val classOfComponent = recordComponent::class.java

        return try {
            Cache(
                classOfComponent.getMethod("getType"),
                classOfComponent.getMethod("getAccessor"),
            )
        } catch (e: NoSuchMethodException) {
            Cache(null, null)
        }
    }

    private fun initCache(recordComponent: Any): Cache {
        var cache = this._cache
        if (cache == null) {
            cache = buildCache(recordComponent)
            this._cache = cache
        }
        return cache

    }

    fun loadGetType(recordComponent: Any): Class<*>? {
        val cache = initCache(recordComponent)
        val getType = cache.getType ?: return null
        return getType.invoke(recordComponent) as Class<*>
    }

    fun loadGetAccessor(recordComponent: Any): Method? {
        val cache = initCache(recordComponent)
        val getType = cache.getAccessor ?: return null
        return getType.invoke(recordComponent) as Method
    }
}
