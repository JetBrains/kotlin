/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.backend

import org.jetbrains.kotlin.bir.*
import org.jetbrains.kotlin.bir.declarations.*
import org.jetbrains.kotlin.bir.symbols.*
import kotlin.reflect.KProperty1

context(BirBackendContext)
abstract class BirLoweringPhase : BirPhase {
    abstract fun lower(module: BirModuleFragment)

    protected fun <E : BirElement> registerIndexKey(
        elementType: BirElementType<E>,
        includeExternalModules: Boolean,
    ): BirElementsIndexKey<E> = registerIndexKey(elementType, null, includeExternalModules)

    /**
     * Registers a handle which can be used later to obtain all [BirElement]s of specific class,
     * and matching a given condition.
     *
     * ### Examples:
     *
     * ```kotlin
     * // match all inline functions
     * val inlineFunctions = registerIndexKey(BirSimpleFunction, false) { it.isInline }
     * // match all declarations with multiple annotations
     * registerIndexKey(BirDeclaration, false) { it.annotations.size > 1 }
     * // match all declarations with @kotlin.JvmStatic
     * registerIndexKey(BirDeclaration, false) { it.hasAnnotation(JvmStaticAnnotation) }
     * ```
     * ### Performance considerations
     *
     * On one hand, the [condition] function should pass for as little elements as possible, so that only
     * a few elements are kept in caches, and retried later on.
     * On the other, the [condition] function should be as fast and small as possible, because it may be
     * called many times, alongside many other pieces of code.
     *
     * The intended usage is to quickly filter out most elements by checking some basic properties, and
     * perform other heavy checks manually, on elements returned by [getAllElementsWithIndex].
     *
     * In particular, [condition] should try to avoid reading properties of any other element than the provided one,
     * if possible. Such a case adds some overhead, including triggering more re-computations whenever
     * any other element touched inside [condition] changes. However, simply comparing references to elements
     * without dereferencing them is fine. Example:
     *
     * ```kotlin
     * // match all reads (in the compiled module) of all volatile properties (from all modules)
     * val volatileReads = registerIndexKey(BirGetValue, false) { getValue ->
     *     getValue.symbol.owner.let {
     *         it is BirProperty && it.isVar && it.hasAnnotation(VolatileAnnotation)
     *     }
     * }
     * ```
     * In this case, except for the mentioned overhead, checks performed on the read properties
     * will likely also be doubled, because there are ususally more [BirGetValue]s than [BirProperty]s.
     * Instead, it is advised to use [BirElement.getBackReferences]:
     *
     * ```kotlin
     * val volatileProperties = registerIndexKey(BirProperty, true) {
     *     it.isVar && it.hasAnnotation(VolatileAnnotation)
     * }
     * val valueReads = registerBackReferencesKey(BirGetValue) { it.symbol.owner }
     *
     * val volatileReads = getAllElementsWithIndex(volatileProperties)
     *     .flatMap { it.getBackReferences(valueReads) }
     * ```
     *
     * However, in the case of simple checks, like in the example below, both approaches may be equally fine:
     *
     * ```kotlin
     * val inlineFunctionCalls = registerIndexKey(BirCall, false) {
     *     it.symbol.owner.isInline
     * }
     * ```
     *
     * @param elementType Only check elements of this and derived classes. May be [BirElement] to check all.
     *
     * @param condition Predicate deciding whether to include a given element in this index.
     *
     * The function must be pure. It should not depend on any mutable state, except for
     * properties of [BirElement]s (such as [BirField.type]) and[BirChildElementList]s.
     * @param includeExternalModules Whether to check [BirElement]s from library modules as well, or only the module being compiled.
     *
     * Note: in the case of external modules, only elements being directly referenced by the code in the compiled module are checked
     * (e.g. only the [BirFunction]s actually being called from the compiled code, but not their bodies nor parameters). This is because
     * they may be represented using lazy IR, and lazy IR is not being expanded while indexing.
     *
     * @return Token which may be passed to [getAllElementsWithIndex].
     *
     * @see getAllElementsWithIndex
     */
    protected inline fun <E : BirElement> registerIndexKey(
        elementType: BirElementType<E>,
        includeExternalModules: Boolean,
        crossinline condition: (E) -> Boolean,
    ): BirElementsIndexKey<E> = registerIndexKey(elementType, { element ->
        @Suppress("UNCHECKED_CAST")
        condition(element as E)
    }, includeExternalModules)

    @PublishedApi
    internal fun <E : BirElement> registerIndexKey(
        elementType: BirElementType<E>,
        condition: BirElementIndexMatcher?,
        includeExternalModules: Boolean,
    ): BirElementsIndexKey<E> {
        val key = BirElementsIndexKey<E>(condition, elementType)
        compiledBir.registerElementIndexingKey(key)
        if (includeExternalModules) {
            externalModulesBir.registerElementIndexingKey(key)
        }

        return key
    }

    /**
     * Registers a handle which can be used later to obtain all [BirElement]s which reference
     * some other [BirElement], in a specified way.
     *
     * ### Examples:
     *
     * ```kotlin
     * val valueReads = registerBackReferencesKey(BirGetValue) { it.symbol.owner }
     *
     * val birProperty: BirProperty = // ...
     * birProperty.getBackReferences(valueReads).forEach {
     *     assert(it is BirGetValue && it.symbol.owner == birProperty)
     * }
     * ```
     *
     * @param elementType The type of referencing element. All elements returned by [BirElement.getBackReferences] will be of this type.
     * In other words, only run [getBackReference] on elements of this and derived classes. May be [BirElement].
     *
     * @param getBackReference Function to obtain a forward reference to some other element. [BirElement.getBackReferences] will return
     * a reverse connection, this is, all elements for which this function returned the instance of receiver.
     * May return null, to not record any forward reference. In case of multiple forward references, use [registerBackReferencesKey].
     *
     * The function must be pure. It should not depend on any mutable state, except for
     * properties of [BirElement]s (such as [BirField.type]) and[BirChildElementList]s.
     *
     * @return Token which may be passed to [BirElement.getBackReferences].
     */
    protected inline fun <reified E : BirElement, R : BirElement> registerBackReferencesKey(
        elementType: BirElementType<E>,
        crossinline getBackReference: (E) -> R?,
    ): BirElementBackReferencesKey<E, R> = registerBackReferencesKey<E, R>(elementType, null, object : BirElementBackReferenceRecorder<R> {
        context(BirElementBackReferenceRecorderScope)
        override fun recordBackReferences(element: BirElementBase) {
            if (element is E) {
                recordReference(getBackReference(element))
            }
        }
    })

    @JvmName("registerBackReferencesKeyWithProperty")
    protected inline fun <reified E : BirElement, R : BirElement> registerBackReferencesKey(
        elementType: BirElementType<E>,
        forwardReferenceProperty: KProperty1<E, R>,
    ): BirElementBackReferencesKey<E, R> =
        registerBackReferencesKey<E, R>(elementType, forwardReferenceProperty, object : BirElementBackReferenceRecorder<R> {
            context(BirElementBackReferenceRecorderScope)
            override fun recordBackReferences(element: BirElementBase) {
                if (element is E) {
                    recordReference(forwardReferenceProperty.get(element))
                }
            }
        })

    @JvmName("registerBackReferencesKeyWithSymbolProperty")
    @Suppress("UNCHECKED_CAST")
    protected inline fun <reified E : BirElement, R : BirElement, S : BirTypedSymbol<R>> registerBackReferencesKey(
        elementType: BirElementType<E>,
        forwardReferenceProperty: KProperty1<E, S>,
    ) = registerBackReferencesKeyWithUntypedSymbolProperty<E>(elementType, forwardReferenceProperty)
            as BirElementBackReferencesKey<E, R>

    // xxx: those overloads overcome the inability to define all [BirSymbol]s as generic
    @Suppress("UNCHECKED_CAST")
    protected inline fun <reified E : BirElement> registerBackReferencesKey_functionSymbol(
        elementType: BirElementType<E>,
        forwardReferenceProperty: KProperty1<E, BirFunctionSymbol>,
    ) = registerBackReferencesKeyWithUntypedSymbolProperty<E>(elementType, forwardReferenceProperty)
            as BirElementBackReferencesKey<E, BirFunction>

    @Suppress("UNCHECKED_CAST")
    protected inline fun <reified E : BirElement> registerBackReferencesKey_returnTargetSymbol(
        elementType: BirElementType<E>,
        forwardReferenceProperty: KProperty1<E, BirReturnTargetSymbol>,
    ) = registerBackReferencesKeyWithUntypedSymbolProperty<E>(elementType, forwardReferenceProperty)
            as BirElementBackReferencesKey<E, BirReturnTarget>

    @Suppress("UNCHECKED_CAST")
    protected inline fun <reified E : BirElement> registerBackReferencesKey_valueSymbol(
        elementType: BirElementType<E>,
        forwardReferenceProperty: KProperty1<E, BirValueSymbol>,
    ) = registerBackReferencesKeyWithUntypedSymbolProperty<E>(elementType, forwardReferenceProperty)
            as BirElementBackReferencesKey<E, BirValueDeclaration>


    protected inline fun <reified E : BirElement> registerBackReferencesKeyWithUntypedSymbolProperty(
        elementType: BirElementType<E>,
        forwardReferenceProperty: KProperty1<E, BirSymbol>,
    ): BirElementBackReferencesKey<E, BirElement> = registerBackReferencesKey<E, BirElement>(
        elementType, forwardReferenceProperty
    ) { element ->
        if (element is E) {
            val symbol = forwardReferenceProperty.get(element)
            recordReference(symbol.owner)
        }
    }

    /**
     * Same as [registerBackReferencesKey], but allows to record multiple forward references for a given element.
     */
    protected inline fun <reified E : BirElement, R : BirElement> registerMultipleBackReferencesKey(
        elementType: BirElementType<E>,
        crossinline getBackReferences: context(BirElementBackReferenceRecorderScope) (E) -> Unit,
    ): BirElementBackReferencesKey<E, R> = registerBackReferencesKey<E, R>(elementType, null, object : BirElementBackReferenceRecorder<R> {
        context(BirElementBackReferenceRecorderScope)
        override fun recordBackReferences(element: BirElementBase) {
            if (element is E) {
                getBackReferences(this@BirElementBackReferenceRecorderScope, element)
            }
        }
    })

    @PublishedApi
    internal fun <E : BirElement, R : BirElement> registerBackReferencesKey(
        elementType: BirElementType<E>,
        forwardReferenceProperty: KProperty1<E, *>?,
        getBackReference: BirElementBackReferenceRecorder<R>,
    ): BirElementBackReferencesKey<E, R> {
        val key = BirElementBackReferencesKey<E, R>(getBackReference, elementType, forwardReferenceProperty)
        compiledBir.registerElementBackReferencesKey(key)
        return key
    }


    /**
     * Gets all elements matching a specified index key, either from the compiled module and/or
     * other modules, as specified in [registerIndexKey].
     *
     * @see BirDatabase.getElementsWithIndex
     */
    protected fun <E : BirElement> getAllElementsWithIndex(key: BirElementsIndexKey<E>): Sequence<E> {
        var elements: Sequence<E> = compiledBir.getElementsWithIndex(key)
        if (externalModulesBir.hasIndex(key)) {
            elements += externalModulesBir.getElementsWithIndex(key)
        }
        return elements
    }


    protected fun <E : BirElement, T> createLocalIrProperty(elementType: BirElementType<E>): BirDynamicPropertyAccessToken<E, T> {
        return PhaseLocalBirDynamicProperty<E, T>(elementType, dynamicPropertyManager, this)
    }


    protected fun <T> lz(initializer: () -> T) = lazy(LazyThreadSafetyMode.NONE, initializer)
}