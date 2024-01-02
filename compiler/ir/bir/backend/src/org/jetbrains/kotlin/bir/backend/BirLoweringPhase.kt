/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.backend

import org.jetbrains.kotlin.bir.*
import org.jetbrains.kotlin.bir.declarations.BirModuleFragment

context(BirBackendContext)
abstract class BirLoweringPhase {
    abstract fun lower(module: BirModuleFragment)

    protected fun <E : BirElement> registerIndexKey(
        elementClass: BirElementClass<E>,
        includeExternalModules: Boolean,
    ): BirElementsIndexKey<E> = registerIndexKey(elementClass, null, includeExternalModules)

    protected inline fun <E : BirElement> registerIndexKey(
        elementClass: BirElementClass<E>,
        includeExternalModules: Boolean,
        crossinline condition: (E) -> Boolean,
    ): BirElementsIndexKey<E> = registerIndexKey(elementClass, { element ->
        @Suppress("UNCHECKED_CAST")
        condition(element as E)
    }, includeExternalModules)

    @PublishedApi
    internal fun <E : BirElement> registerIndexKey(
        elementClass: BirElementClass<E>,
        condition: BirElementIndexMatcher?,
        includeExternalModules: Boolean,
    ): BirElementsIndexKey<E> {
        val key = BirElementsIndexKey<E>(condition, elementClass)
        compiledBir.registerElementIndexingKey(key)
        if (includeExternalModules) {
            externalModulesBir.registerElementIndexingKey(key)
        }

        return key
    }

    protected fun <E : BirElement, R : BirElement> registerBackReferencesKey(
        elementClass: BirElementClass<E>,
        getBackReference: BirElementBackReferenceRecorder<R>,
    ): BirElementBackReferencesKey<E, R> {
        val key = BirElementBackReferencesKey<E, R>(getBackReference, elementClass)
        compiledBir.registerElementBackReferencesKey(key)
        return key
    }

    protected inline fun <reified E : BirElement, R : BirElement> registerBackReferencesKey(
        elementClass: BirElementClass<E>,
        crossinline block: (E) -> R?,
    ): BirElementBackReferencesKey<E, R> = registerBackReferencesKey<E, R>(elementClass, object : BirElementBackReferenceRecorder<R> {
        context(BirElementBackReferenceRecorderScope)
        override fun recordBackReferences(element: BirElementBase) {
            if (element is E) {
                recordReference(block(element))
            }
        }
    })

    protected inline fun <reified E : BirElement, R : BirElement> registerMultipleBackReferencesKey(
        elementClass: BirElementClass<E>,
        crossinline getBackReferences: context(BirElementBackReferenceRecorderScope) (E) -> Unit,
    ): BirElementBackReferencesKey<E, R> = registerBackReferencesKey<E, R>(elementClass, object : BirElementBackReferenceRecorder<R> {
        context(BirElementBackReferenceRecorderScope)
        override fun recordBackReferences(element: BirElementBase) {
            if (element is E) {
                getBackReferences(this@BirElementBackReferenceRecorderScope, element)
            }
        }
    })

    protected fun <E : BirElement> getAllElementsWithIndex(key: BirElementsIndexKey<E>): Sequence<E> {
        var elements = compiledBir.getElementsWithIndex(key)
        if (externalModulesBir.hasIndex(key)) {
            elements += externalModulesBir.getElementsWithIndex(key)
        }
        return elements
    }


    protected fun <E : BirElement, T> acquireProperty(property: BirElementDynamicPropertyKey<E, T>): BirElementDynamicPropertyToken<E, T> {
        return dynamicPropertyManager.acquireProperty(property)
    }

    protected inline fun <reified E : BirElement, T> acquireTemporaryProperty(): BirElementDynamicPropertyToken<E, T> {
        val property = BirElementDynamicPropertyKey<E, T>()
        return acquireProperty(property)
    }


    protected fun <T> lz(initializer: () -> T) = lazy(LazyThreadSafetyMode.NONE, initializer)
}