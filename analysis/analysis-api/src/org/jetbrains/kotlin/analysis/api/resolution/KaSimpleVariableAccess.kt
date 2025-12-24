/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.resolution

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.psi.KtExpression

/**
 * For a [variable access][KaSimpleVariableAccessCall], [KaSimpleVariableAccess] determines the kind of access to the variable (read or
 * write), alongside additional information.
 */
@Deprecated(
    message = "Use 'KaVariableAccessCall.Kind' instead",
    replaceWith = ReplaceWith(
        "KaVariableAccessCall.Kind",
        imports = ["org.jetbrains.kotlin.analysis.api.resolution.KaVariableAccessCall"],
    ),
)
public sealed interface KaSimpleVariableAccess {
    /**
     * The [variable access][KaSimpleVariableAccessCall] reads the variable.
     */
    @Deprecated(
        message = "Use 'KaVariableAccessCall.Kind.Read' instead",
        replaceWith = ReplaceWith(
            "KaVariableAccessCall.Kind.Read",
            imports = ["org.jetbrains.kotlin.analysis.api.resolution.KaVariableAccessCall"],
        ),
    )
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface Read : @Suppress("DEPRECATION") KaSimpleVariableAccess

    /**
     * The [variable access][KaSimpleVariableAccessCall] writes to the variable.
     */
    @Deprecated(
        message = "Use 'KaVariableAccessCall.Kind.Write' instead",
        replaceWith = ReplaceWith(
            "KaVariableAccessCall.Kind.Write",
            imports = ["org.jetbrains.kotlin.analysis.api.resolution.KaVariableAccessCall"],
        ),
    )
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface Write : @Suppress("DEPRECATION") KaSimpleVariableAccess {
        /**
         * A [KtExpression] that represents the new value which is assigned to this variable, or `null` if the assignment is incomplete and
         * lacks the new value.
         */
        public val value: KtExpression?
    }
}
