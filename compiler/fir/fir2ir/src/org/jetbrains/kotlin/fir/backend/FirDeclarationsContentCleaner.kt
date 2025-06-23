/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.declarations.FirAnonymousInitializer
import org.jetbrains.kotlin.fir.declarations.FirAnonymousObject
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.declarations.FirEnumEntry
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.expressions.builder.buildExpressionStub
import org.jetbrains.kotlin.fir.expressions.impl.FirExpressionStub
import org.jetbrains.kotlin.fir.types.resolvedType

class FirDeclarationsContentCleaner(configuration: Fir2IrConfiguration) {
    // Don't modify the fir tree in...
    private val doNotClean: Boolean =
        configuration.allowNonCachedDeclarations || // IDE debugger mode
                configuration.skipBodies // KAPT mode

    fun cleanFile(file: FirFile) {
        if (doNotClean) return
        file.replaceControlFlowGraphReference(null)
    }

    fun cleanClass(regularClass: FirRegularClass) {
        if (doNotClean) return
        regularClass.replaceControlFlowGraphReference(null)
    }

    fun cleanAnonymousObject(anonymousObject: FirAnonymousObject) {
        if (doNotClean) return
        anonymousObject.replaceControlFlowGraphReference(null)
    }

    fun cleanConstructor(constructor: FirConstructor) {
        if (doNotClean) return
        constructor.replaceControlFlowGraphReference(null)
        constructor.replaceBody(null)
        constructor.replaceDelegatedConstructor(null)
    }

    fun cleanSimpleFunction(simpleFunction: FirSimpleFunction) {
        if (doNotClean) return
        simpleFunction.replaceControlFlowGraphReference(null)
        simpleFunction.replaceBody(null)
    }

    fun cleanAnonymousFunction(anonymousFunction: FirAnonymousFunction) {
        if (doNotClean) return
        anonymousFunction.replaceControlFlowGraphReference(null)
        anonymousFunction.replaceBody(null)
    }

    fun cleanValueParameter(valueParameter: FirValueParameter) {
        if (doNotClean) return
        valueParameter.replaceControlFlowGraphReference(null)
        valueParameter.defaultValue?.let { defaultValue ->
            val stub = buildExpressionStub {
                source = defaultValue.source
                coneTypeOrNull = defaultValue.resolvedType
            }
            valueParameter.replaceInitializer(stub)
        }
    }

    fun cleanProperty(property: FirProperty) {
        if (doNotClean) return
        property.replaceInitializer(null)
        property.replaceControlFlowGraphReference(null)
        property.getter?.let { cleanPropertyAccessor(it) }
        property.setter?.let { cleanPropertyAccessor(it) }
    }

    fun cleanPropertyAccessor(propertyAccessor: FirPropertyAccessor) {
        if (doNotClean) return
        propertyAccessor.replaceControlFlowGraphReference(null)
        propertyAccessor.replaceBody(null)
    }

    fun cleanEnumEntry(enumEntry: FirEnumEntry) {
        if (doNotClean) return
        enumEntry.replaceInitializer(null)
    }

    fun cleanAnonymousInitializer(anonymousInitializer: FirAnonymousInitializer) {
        if (doNotClean) return
        anonymousInitializer.replaceControlFlowGraphReference(null)
        anonymousInitializer.replaceBody(null)
    }
}
