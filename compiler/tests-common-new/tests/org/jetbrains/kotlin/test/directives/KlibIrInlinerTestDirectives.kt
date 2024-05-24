/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.directives

import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer

object KlibIrInlinerTestDirectives : SimpleDirectivesContainer() {
    val ENABLE_IR_VISIBILITY_CHECKS_AFTER_INLINING by directive(
        description = """
        Check for visibility violation when validating IR after inlining.
        Equivalent to passing the '-Xverify-ir-visibility-after-inlining' CLI flag.
        
        This directive is opt-in rather than opt-out (like ${CodegenTestDirectives.DISABLE_IR_VISIBILITY_CHECKS})
        because right now most test pass with visibility checks enabled before lowering, but enabling these checks
        after inlining by default will cause some tests to fail, because some lowerings that are run before inlining
        generate calls to internal intrinsics (KT-70295).
        """.trimIndent()
    )

    val KLIB_SYNTHETIC_ACCESSORS_WITH_NARROWED_VISIBILITY by directive(
        """
            Narrow the visibility of generated synthetic accessors to _internal_" +
            if such accessors are only used in inline functions that are not a part of public ABI
            Equivalent to passing the '-Xsynthetic-accessors-with-narrowed-visibility' CLI flag.
        """.trimIndent()
    )

    val DUMP_KLIB_SYNTHETIC_ACCESSORS by directive(
        """
            Enable dumping synthetic accessors and their use-sites immediately generation.
            This directive makes sense only for KLIB-based backends.
            Equivalent to passing the '-Xdump-synthetic-accessors-to=<tempDir>/synthetic-accessors' CLI flag.
        """.trimIndent()
    )

    val IDENTICAL_KLIB_SYNTHETIC_ACCESSOR_DUMPS by directive(
        """
            Normally, there should be different dumps of synthetic accessors generated with and without
            narrowing visibility (see $KLIB_SYNTHETIC_ACCESSORS_WITH_NARROWED_VISIBILITY directive
            for details). But sometimes these dumps are identical. In such cases with this directive
            it's possible to have just one dump file.
        """.trimIndent()
    )

    val SKIP_UNBOUND_IR_SERIALIZATION by directive(
        """
            This is a directive to skip some test data files in unbound IR serialization tests.
            
            Some tests are known to have call sites of local fake overrides inside inline functions.
            Currently, serialization of such call sites is not supported. It should be supported in KT-72296.

            Other tests use exposure of private types from internal inline functions. This is already a compiler
            warning in 2.1.0 (KT-69681), but soon will become a compiler error (KT-70916).
        """.trimIndent()
    )
}
