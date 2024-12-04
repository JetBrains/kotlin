/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.common

import org.jetbrains.kotlin.cli.common.arguments.CommonKlibBasedCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.IrVerificationMode
import org.jetbrains.kotlin.config.KlibConfigurationKeys

fun CompilerConfiguration.setupCommonKlibArguments(
    arguments: CommonKlibBasedCompilerArguments
) {
    val messageCollector = getNotNull(CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY)

    if (arguments.verifyIrVisibilityAfterInlining) {
        put(KlibConfigurationKeys.ENABLE_IR_VISIBILITY_CHECKS_AFTER_INLINING, true)

        val irVerificationMode = get(CommonConfigurationKeys.VERIFY_IR, IrVerificationMode.NONE)
        if (irVerificationMode == IrVerificationMode.NONE) {
            messageCollector.report(
                CompilerMessageSeverity.WARNING,
                "'-Xverify-ir-visibility-after-inlining' has no effect unless '-Xverify-ir=warning' or '-Xverify-ir=error' is specified"
            )
        }
    }
}
