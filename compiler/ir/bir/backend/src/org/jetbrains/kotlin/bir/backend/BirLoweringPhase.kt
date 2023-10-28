/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.backend/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.jetbrains.kotlin.bir.*
import org.jetbrains.kotlin.bir.declarations.BirModuleFragment

/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

context(BirBackendContext)
abstract class BirLoweringPhase {
    abstract operator fun invoke(module: BirModuleFragment)

    protected inline fun <reified E : BirElement> registerIndexKey(
        includeOtherModules: Boolean,
        crossinline condition: (E) -> Boolean,
    ): BirElementsIndexKey<E> =
        registerIndexKey<E>(includeOtherModules, { element -> condition(element as E) }, E::class.java)

    protected inline fun <reified E : BirElement> registerIndexKey(includeOtherModules: Boolean): BirElementsIndexKey<E> =
        registerIndexKey<E>(includeOtherModules) { true }

    protected fun <E : BirElement> registerIndexKey(
        includeOtherModules: Boolean,
        condition: BirElementIndexMatcher,
        elementClass: Class<*>,
    ): BirElementsIndexKey<E> {
        val key = BirElementsIndexKey<E>(condition, elementClass, includeOtherModules)
        compiledBir.registerElementIndexingKey(key)
        return key
    }


    protected inline fun <reified E : BirElement> registerBackReferencesKey(
        crossinline block: context(BirElementBackReferenceRecorderScope) (E) -> Unit,
    ): BirElementBackReferencesKey<E> = registerBackReferencesKey<E>(object : BirElementBackReferenceRecorder {
        context(BirElementBackReferenceRecorderScope)
        override fun recordBackReferences(element: BirElementBase) {
            block(this@BirElementBackReferenceRecorderScope, element as E)
        }
    }, E::class.java)

    protected fun <E : BirElement> registerBackReferencesKey(
        block: BirElementBackReferenceRecorder,
        elementClass: Class<*>,
    ): BirElementBackReferencesKey<E> {
        val key = BirElementBackReferencesKey<E>(block, elementClass)
        compiledBir.registerElementBackReferencesKey(key)
        return key
    }


    protected fun <E : BirElement, T> acquireProperty(property: BirElementDynamicPropertyKey<E, T>): BirElementDynamicPropertyToken<E, T> {
        return dynamicPropertyManager.acquireProperty(property)
    }

    protected inline fun <reified E : BirElement, T> acquireTemporaryProperty(): BirElementDynamicPropertyToken<E, T> {
        val property = BirElementDynamicPropertyKey<E, T>()
        return acquireProperty(property)
    }


    protected fun<T> lz(initializer: () -> T) = lazy(LazyThreadSafetyMode.NONE, initializer)
}