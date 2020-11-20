/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.fir.utils.ArrayMap
import org.jetbrains.kotlin.fir.utils.AttributeArrayOwner
import org.jetbrains.kotlin.fir.utils.NullableArrayMapAccessor
import org.jetbrains.kotlin.fir.utils.TypeRegistry
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

abstract class FirDeclarationDataKey

class FirDeclarationAttributes : AttributeArrayOwner<FirDeclarationDataKey, Any> {
    override val typeRegistry: TypeRegistry<FirDeclarationDataKey, Any>
        get() = FirDeclarationDataRegistry

    constructor() : super()
    private constructor(arrayMap: ArrayMap<Any>) : super(arrayMap)

    internal operator fun set(key: KClass<out FirDeclarationDataKey>, value: Any?) {
        if (value == null) {
            removeComponent(key)
        } else {
            registerComponent(key, value)
        }
    }

    fun copy(): FirDeclarationAttributes = FirDeclarationAttributes(arrayMap.copy())
}

/*
 * Example of adding new attribute for declaration:
 *
 *    object SomeKey : FirDeclarationDataKey()
 *    var FirDeclaration.someString: String? by FirDeclarationDataRegistry.data(SomeKey)
 */
object FirDeclarationDataRegistry : TypeRegistry<FirDeclarationDataKey, Any>() {
    fun <K : FirDeclarationDataKey, V : Any> data(key: K): ReadWriteProperty<FirDeclaration, V?> {
        val kClass = key::class
        return DeclarationDataAccessor(generateNullableAccessor(kClass), kClass)
    }

    fun <K : FirDeclarationDataKey, V : Any> attributesAccessor(key: K): ReadWriteProperty<FirDeclarationAttributes, V?> {
        val kClass = key::class
        return AttributeDataAccessor(generateNullableAccessor(kClass), kClass)
    }

    private class DeclarationDataAccessor<V : Any>(
        val dataAccessor: NullableArrayMapAccessor<FirDeclarationDataKey, Any, V>,
        val key: KClass<out FirDeclarationDataKey>
    ) : ReadWriteProperty<FirDeclaration, V?> {
        override fun getValue(thisRef: FirDeclaration, property: KProperty<*>): V? {
            return dataAccessor.getValue(thisRef.attributes, property)
        }

        override fun setValue(thisRef: FirDeclaration, property: KProperty<*>, value: V?) {
            thisRef.attributes[key] = value
        }
    }

    private class AttributeDataAccessor<V : Any>(
        val dataAccessor: NullableArrayMapAccessor<FirDeclarationDataKey, Any, V>,
        val key: KClass<out FirDeclarationDataKey>
    ) : ReadWriteProperty<FirDeclarationAttributes, V?> {
        override fun getValue(thisRef: FirDeclarationAttributes, property: KProperty<*>): V? {
            return dataAccessor.getValue(thisRef, property)
        }

        override fun setValue(thisRef: FirDeclarationAttributes, property: KProperty<*>, value: V?) {
            thisRef[key] = value
        }
    }
}
