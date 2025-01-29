/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.configuration

import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.HandlersStepBuilder
import org.jetbrains.kotlin.test.backend.handlers.IrPrettyKotlinDumpHandler
import org.jetbrains.kotlin.test.backend.handlers.IrSourceRangesDumpHandler
import org.jetbrains.kotlin.test.backend.handlers.IrTextDumpHandler
import org.jetbrains.kotlin.test.backend.handlers.IrTreeVerifierHandler
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.configureFirHandlersStep
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.DUMP_IR
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.DUMP_KT_IR
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives.DIAGNOSTICS
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives.REPORT_ONLY_EXPLICITLY_DEFINED_DEBUG_INFO
import org.jetbrains.kotlin.test.directives.KlibAbiDumpDirectives.DUMP_KLIB_ABI
import org.jetbrains.kotlin.test.directives.KlibAbiDumpDirectives.KlibAbiDumpMode
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.LINK_VIA_SIGNATURES_K1
import org.jetbrains.kotlin.test.directives.configureFirParser
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirDumpHandler
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirScopeDumpHandler
import org.jetbrains.kotlin.test.model.BackendKinds

fun TestConfigurationBuilder.setupDefaultDirectivesForIrTextTest() {
    defaultDirectives {
        +DUMP_IR
        +DUMP_KT_IR
        +LINK_VIA_SIGNATURES_K1
        +REPORT_ONLY_EXPLICITLY_DEFINED_DEBUG_INFO
        DIAGNOSTICS with "-warnings"
        DUMP_KLIB_ABI with KlibAbiDumpMode.DEFAULT
    }
}

fun HandlersStepBuilder<IrBackendInput, BackendKinds.IrBackend>.setupIrTextDumpHandlers() {
    useHandlers(
        ::IrTextDumpHandler,
        ::IrTreeVerifierHandler,
        ::IrPrettyKotlinDumpHandler,
        ::IrSourceRangesDumpHandler,
    )
}

fun TestConfigurationBuilder.additionalK2ConfigurationForIrTextTest(parser: FirParser) {
    configureFirParser(parser)

    configureFirHandlersStep {
        useHandlersAtFirst(
            ::FirDumpHandler,
            ::FirScopeDumpHandler,
        )
    }

    forTestsMatching("compiler/testData/ir/irText/properties/backingField/*") {
        defaultDirectives {
            LanguageSettingsDirectives.LANGUAGE with "+ExplicitBackingFields"
        }
    }
}
