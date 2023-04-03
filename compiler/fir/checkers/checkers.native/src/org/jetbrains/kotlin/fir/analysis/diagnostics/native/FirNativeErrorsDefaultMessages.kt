/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.diagnostics.native

import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.KtDiagnosticRenderers.TO_STRING
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirDiagnosticRenderers.SYMBOL
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirDiagnosticRenderers.SYMBOLS
import org.jetbrains.kotlin.fir.analysis.diagnostics.native.FirNativeErrors.EMPTY_OBJC_NAME
import org.jetbrains.kotlin.fir.analysis.diagnostics.native.FirNativeErrors.INAPPLICABLE_EXACT_OBJC_NAME
import org.jetbrains.kotlin.fir.analysis.diagnostics.native.FirNativeErrors.INAPPLICABLE_OBJC_NAME
import org.jetbrains.kotlin.fir.analysis.diagnostics.native.FirNativeErrors.INAPPLICABLE_SHARED_IMMUTABLE_PROPERTY
import org.jetbrains.kotlin.fir.analysis.diagnostics.native.FirNativeErrors.INAPPLICABLE_SHARED_IMMUTABLE_TOP_LEVEL
import org.jetbrains.kotlin.fir.analysis.diagnostics.native.FirNativeErrors.INAPPLICABLE_THREAD_LOCAL
import org.jetbrains.kotlin.fir.analysis.diagnostics.native.FirNativeErrors.INAPPLICABLE_THREAD_LOCAL_TOP_LEVEL
import org.jetbrains.kotlin.fir.analysis.diagnostics.native.FirNativeErrors.INCOMPATIBLE_OBJC_NAME_OVERRIDE
import org.jetbrains.kotlin.fir.analysis.diagnostics.native.FirNativeErrors.INCOMPATIBLE_OBJC_REFINEMENT_OVERRIDE
import org.jetbrains.kotlin.fir.analysis.diagnostics.native.FirNativeErrors.INCOMPATIBLE_THROWS_INHERITED
import org.jetbrains.kotlin.fir.analysis.diagnostics.native.FirNativeErrors.INCOMPATIBLE_THROWS_OVERRIDE
import org.jetbrains.kotlin.fir.analysis.diagnostics.native.FirNativeErrors.INVALID_CHARACTERS_NATIVE
import org.jetbrains.kotlin.fir.analysis.diagnostics.native.FirNativeErrors.INVALID_OBJC_NAME
import org.jetbrains.kotlin.fir.analysis.diagnostics.native.FirNativeErrors.INVALID_OBJC_NAME_CHARS
import org.jetbrains.kotlin.fir.analysis.diagnostics.native.FirNativeErrors.INVALID_OBJC_NAME_FIRST_CHAR
import org.jetbrains.kotlin.fir.analysis.diagnostics.native.FirNativeErrors.INVALID_OBJC_REFINEMENT_TARGETS
import org.jetbrains.kotlin.fir.analysis.diagnostics.native.FirNativeErrors.MISSING_EXACT_OBJC_NAME
import org.jetbrains.kotlin.fir.analysis.diagnostics.native.FirNativeErrors.MISSING_EXCEPTION_IN_THROWS_ON_SUSPEND
import org.jetbrains.kotlin.fir.analysis.diagnostics.native.FirNativeErrors.REDUNDANT_SWIFT_REFINEMENT
import org.jetbrains.kotlin.fir.analysis.diagnostics.native.FirNativeErrors.NON_LITERAL_OBJC_NAME_ARG
import org.jetbrains.kotlin.fir.analysis.diagnostics.native.FirNativeErrors.THROWS_LIST_EMPTY

object FirNativeErrorsDefaultMessages : BaseDiagnosticRendererFactory() {
    override val MAP = KtDiagnosticFactoryToRendererMap("FIR").also { map ->
        map.put(THROWS_LIST_EMPTY, "Throws must have non-empty class list")
        map.put(INCOMPATIBLE_THROWS_OVERRIDE, "Member overrides different @Throws filter from {0}", SYMBOL)
        map.put(INCOMPATIBLE_THROWS_INHERITED, "Member inherits different @Throws filters from {0}", SYMBOLS)
        map.put(
            MISSING_EXCEPTION_IN_THROWS_ON_SUSPEND, "@Throws on suspend declaration must have {0} (or any of its superclasses) listed",
            TO_STRING
        )
        map.put(
            INAPPLICABLE_SHARED_IMMUTABLE_PROPERTY,
            "@SharedImmutable is applicable only to val with backing field or to property with delegation"
        )
        map.put(INAPPLICABLE_SHARED_IMMUTABLE_TOP_LEVEL, "@SharedImmutable is applicable only to top level declarations")
        map.put(
            INAPPLICABLE_THREAD_LOCAL,
            "@ThreadLocal is applicable only to property with backing field, to property with delegation or to objects"
        )
        map.put(INAPPLICABLE_THREAD_LOCAL_TOP_LEVEL, "@ThreadLocal is applicable only to top level declarations")
        map.put(INVALID_CHARACTERS_NATIVE, "Name {0}", TO_STRING)
        map.put(REDUNDANT_SWIFT_REFINEMENT, "An ObjC refined declaration can't also be refined in Swift")
        map.put(
            INCOMPATIBLE_OBJC_REFINEMENT_OVERRIDE,
            "Refined declaration \"{0}\" overrides declarations with different or no refinement from {1}",
            SYMBOL,
            SYMBOLS
        )
        map.put(
            INVALID_OBJC_REFINEMENT_TARGETS,
            "Refines annotations are only applicable to annotations with targets FUNCTION and/or PROPERTY"
        )
        map.put(INAPPLICABLE_OBJC_NAME, "@ObjCName is not applicable on overrides")
        map.put(INVALID_OBJC_NAME, "@ObjCName should have a name and/or swiftName")
        map.put(EMPTY_OBJC_NAME, "Empty @ObjCName names aren't supported")
        map.put(INVALID_OBJC_NAME_CHARS, "@ObjCName contains illegal characters: {0}", TO_STRING)
        map.put(INVALID_OBJC_NAME_FIRST_CHAR, "@ObjCName contains illegal first characters: {0}", TO_STRING)
        map.put(INCOMPATIBLE_OBJC_NAME_OVERRIDE, "Member \"{0}\" inherits inconsistent @ObjCName from {1}", SYMBOL, SYMBOLS)
        map.put(INAPPLICABLE_EXACT_OBJC_NAME, "Exact @ObjCName is only applicable to classes, objects and interfaces")
        map.put(MISSING_EXACT_OBJC_NAME, "Exact @ObjCName is required to have an ObjC name")
        map.put(NON_LITERAL_OBJC_NAME_ARG, "@ObjCName accepts only literal string and boolean values")
    }
}
