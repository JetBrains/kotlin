/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.fir.FirImplementationDetail
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.hasBackingField
import org.jetbrains.kotlin.fir.declarations.utils.hasBackingFieldAttr
import org.jetbrains.kotlin.fir.expressions.builder.buildExpressionStub
import org.jetbrains.kotlin.fir.serialization.constant.hasConstantValue
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.utils.addToStdlib.runIf

sealed class FirDeclarationsContentCleaner {
    abstract fun cleanFile(file: FirFile)
    abstract fun cleanClass(regularClass: FirRegularClass)
    abstract fun cleanAnonymousObject(anonymousObject: FirAnonymousObject)
    abstract fun cleanConstructor(constructor: FirConstructor)
    abstract fun cleanNamedFunction(namedFunction: FirNamedFunction)
    abstract fun cleanAnonymousFunction(anonymousFunction: FirAnonymousFunction)
    abstract fun cleanValueParameter(valueParameter: FirValueParameter)
    abstract fun cleanProperty(property: FirProperty)
    abstract fun cleanEnumEntry(enumEntry: FirEnumEntry)
    abstract fun cleanAnonymousInitializer(anonymousInitializer: FirAnonymousInitializer)

    companion object {
        context(c: Fir2IrComponents)
        fun create(): FirDeclarationsContentCleaner {
            val configuration = c.configuration
            return when {
                // Don't modify the fir tree in...
                configuration.allowNonCachedDeclarations || // IDE debugger mode
                        configuration.skipBodies || // KAPT mode
                        // In HMPP mode we need to iterate over FIR bodies once again, after all modules are converted to IR in [referenceAllCommonDependencies]
                        configuration.languageVersionSettings.getFlag(AnalysisFlags.hierarchicalMultiplatformCompilation)
                    -> DoNothing

                else -> CleanBodies(c.session)
            }
        }
    }

    object DoNothing : FirDeclarationsContentCleaner() {
        override fun cleanFile(file: FirFile) {}
        override fun cleanClass(regularClass: FirRegularClass) {}
        override fun cleanAnonymousObject(anonymousObject: FirAnonymousObject) {}
        override fun cleanConstructor(constructor: FirConstructor) {}
        override fun cleanNamedFunction(namedFunction: FirNamedFunction) {}
        override fun cleanAnonymousFunction(anonymousFunction: FirAnonymousFunction) {}
        override fun cleanValueParameter(valueParameter: FirValueParameter) {}
        override fun cleanProperty(property: FirProperty) {}
        override fun cleanEnumEntry(enumEntry: FirEnumEntry) {}
        override fun cleanAnonymousInitializer(anonymousInitializer: FirAnonymousInitializer) {}
    }

    class CleanBodies(val session: FirSession) : FirDeclarationsContentCleaner() {
        override fun cleanFile(file: FirFile) {
            file.replaceControlFlowGraphReference(null)
        }

        override fun cleanClass(regularClass: FirRegularClass) {
            regularClass.replaceControlFlowGraphReference(null)
        }

        override fun cleanAnonymousObject(anonymousObject: FirAnonymousObject) {
            anonymousObject.replaceControlFlowGraphReference(null)
        }

        override fun cleanConstructor(constructor: FirConstructor) {
            constructor.replaceControlFlowGraphReference(null)
            constructor.replaceBody(null)
            constructor.replaceDelegatedConstructor(null)
        }

        override fun cleanNamedFunction(namedFunction: FirNamedFunction) {
            namedFunction.replaceControlFlowGraphReference(null)
            namedFunction.replaceBody(null)
        }

        override fun cleanAnonymousFunction(anonymousFunction: FirAnonymousFunction) {
            anonymousFunction.replaceControlFlowGraphReference(null)
            anonymousFunction.replaceBody(null)
        }

        override fun cleanValueParameter(valueParameter: FirValueParameter) {
            valueParameter.replaceControlFlowGraphReference(null)
            valueParameter.defaultValue?.let { defaultValue ->
                val stub = buildExpressionStub {
                    source = defaultValue.source
                    coneTypeOrNull = defaultValue.resolvedType
                }
                valueParameter.replaceInitializer(stub)
            }
        }

        override fun cleanProperty(property: FirProperty) {
            // Since cleanupPropertyAccessor deletes the getter / setter body,
            // it may be no longer possible to compute FirProperty.hasBackingField correctly,
            // so we preserve it here.
            // Correct backing field information may be required when producing metadata in JvmIrCodegen
            // for non-final @Serializable classes.
            @OptIn(FirImplementationDetail::class)
            property.hasBackingFieldAttr = property.hasBackingField

            property.initializer?.let { initializer ->
                val newInitializer = runIf(initializer.hasConstantValue(session)) {
                    buildExpressionStub {
                        source = initializer.source
                        coneTypeOrNull = initializer.resolvedType
                    }
                }
                property.replaceInitializer(newInitializer)
            }
            property.replaceControlFlowGraphReference(null)
            property.getter?.let { cleanPropertyAccessor(it) }
            property.setter?.let { cleanPropertyAccessor(it) }
        }

        private fun cleanPropertyAccessor(propertyAccessor: FirPropertyAccessor) {
            propertyAccessor.replaceControlFlowGraphReference(null)
            propertyAccessor.replaceBody(null)
        }

        override fun cleanEnumEntry(enumEntry: FirEnumEntry) {
            enumEntry.replaceInitializer(null)
        }

        override fun cleanAnonymousInitializer(anonymousInitializer: FirAnonymousInitializer) {
            anonymousInitializer.replaceControlFlowGraphReference(null)
            anonymousInitializer.replaceBody(null)
        }
    }
}
