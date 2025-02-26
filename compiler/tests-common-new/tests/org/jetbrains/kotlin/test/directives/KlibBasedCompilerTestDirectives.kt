/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.directives

import org.jetbrains.kotlin.config.KlibConfigurationKeys
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.handlers.SerializedIrDumpHandler
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer

object KlibBasedCompilerTestDirectives : SimpleDirectivesContainer() {
    val DUMP_KLIB_SYNTHETIC_ACCESSORS by directive(
        """
            Enable dumping synthetic accessors and their use-sites immediately generation.
            This directive makes sense only for KLIB-based backends.
            Equivalent to passing the '-Xdump-synthetic-accessors-to=<tempDir>/synthetic-accessors' CLI flag.
        """.trimIndent()
    )

    // TODO: to be dropped in KT-69941
    val KLIB_SYNTHETIC_ACCESSORS_WITH_NARROWED_VISIBILITY by directive(
        """
            Narrow the visibility of generated synthetic accessors to _internal_" +
            if such accessors are only used in inline functions that are not a part of public ABI
            Equivalent to passing the '-Xsynthetic-accessors-with-narrowed-visibility' CLI flag.
        """.trimIndent()
    )

    // TODO: to be dropped in KT-69941
    val IDENTICAL_KLIB_SYNTHETIC_ACCESSOR_DUMPS by directive(
        """
            Normally, there should be different dumps of synthetic accessors generated with and without
            narrowing visibility (see $KLIB_SYNTHETIC_ACCESSORS_WITH_NARROWED_VISIBILITY directive
            for details). But sometimes these dumps are identical. In such cases with this directive
            it's possible to have just one dump file.
        """.trimIndent()
    )

    val IGNORE_KLIB_SYNTHETIC_ACCESSORS_CHECKS by enumDirective<TargetBackend>(
        "Ignore failures in checking synthetic accessors for the specified backend"
    )

    val SKIP_GENERATING_KLIB by directive(
        description = """
        Skips generating KLIB when running tests
        """
    )

    val SKIP_UNBOUND_IR_SERIALIZATION by directive(
        """
            This is a directive to skip some test data files in unbound IR serialization tests
            (see `AbstractNativeUnboundIrSerializationTest` and generated subclasses).

            Some tests use exposure of private types from internal inline functions. This is already a compiler
            warning in 2.1.0 (KT-69681), but soon will become a compiler error (KT-70916).
        """.trimIndent()
    )

    val SKIP_IR_DESERIALIZATION_CHECKS by directive(
        description = """
        Skips ${SerializedIrDumpHandler::class}, when running a test against the deserialized IR
        """
    )

    // This is "IGNORE"-like test directive.
    val IGNORE_IR_DESERIALIZATION_TEST by enumDirective<TargetBackend>(
        description = "Ignore failures on IR mismatch before Klib serialization vs after Klib deserialization",
    )

    val KLIB_RELATIVE_PATH_BASES by stringDirective(
        description = "Sets ${KlibConfigurationKeys.KLIB_RELATIVE_PATH_BASES}"
    )
}
