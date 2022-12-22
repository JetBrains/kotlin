/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.util.ConeTypeRegistry
import org.jetbrains.kotlin.util.ArrayMap
import org.jetbrains.kotlin.util.AttributeArrayOwner
import org.jetbrains.kotlin.util.NullableArrayMapAccessor
import org.jetbrains.kotlin.util.TypeRegistry
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

abstract class FirDeclarationDataKey

/**
 * Please note that FirDeclarationAttributes itself is thread unsafe, so when you read
 *   or write some attribute you need to ensure that this operation is safe and there
 *   won't be any race condition. You can achieve this by
 * - setting attribute to declaration before it's publication (e.g. in scopes)
 * - setting attribute in one phase and reading it only in following ones (using `ensureResolve` on symbol)
 * - resetting attribute under lock over specific attribute value
 */
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
object FirDeclarationDataRegistry : ConeTypeRegistry<FirDeclarationDataKey, Any>() {
    fun <K : FirDeclarationDataKey> data(key: K): DeclarationDataAccessor {
        val kClass = key::class
        return DeclarationDataAccessor(generateAnyNullableAccessor(kClass), kClass)
    }

    fun <K : FirDeclarationDataKey> symbolAccessor(key: K): SymbolDataAccessor {
        val kClass = key::class
        return SymbolDataAccessor(generateAnyNullableAccessor(kClass), kClass)
    }

    fun <K : FirDeclarationDataKey, V : Any> attributesAccessor(key: K): ReadWriteProperty<FirDeclarationAttributes, V?> {
        val kClass = key::class
        return AttributeDataAccessor(generateNullableAccessor(kClass), kClass)
    }

    class DeclarationDataAccessor(
        private val dataAccessor: NullableArrayMapAccessor<FirDeclarationDataKey, Any, *>,
        val key: KClass<out FirDeclarationDataKey>
    ) {
        operator fun <V> getValue(thisRef: FirDeclaration, property: KProperty<*>): V? {
            @Suppress("UNCHECKED_CAST")
            return dataAccessor.getValue(thisRef.attributes, property) as? V
        }

        operator fun <V> setValue(thisRef: FirDeclaration, property: KProperty<*>, value: V?) {
            thisRef.attributes[key] = value
        }
    }

    class SymbolDataAccessor(
        private val dataAccessor: NullableArrayMapAccessor<FirDeclarationDataKey, Any, *>,
        val key: KClass<out FirDeclarationDataKey>
    ) {
        operator fun <V> getValue(thisRef: FirBasedSymbol<*>, property: KProperty<*>): V? {
            @Suppress("UNCHECKED_CAST")
            return dataAccessor.getValue(thisRef.fir.attributes, property) as? V
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
