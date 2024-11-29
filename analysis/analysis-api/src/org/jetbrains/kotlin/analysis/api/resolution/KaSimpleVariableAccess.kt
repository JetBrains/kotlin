/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.resolution

import org.jetbrains.kotlin.psi.KtExpression

/**
 * For a [variable access][KaSimpleVariableAccessCall], [KaSimpleVariableAccess] determines the kind of access to the variable (read or
 * write), alongside additional information.
 */
public sealed interface KaSimpleVariableAccess {
    /**
     * The [variable access][KaSimpleVariableAccessCall] reads the variable.
     */
    public interface Read : KaSimpleVariableAccess

    /**
     * The [variable access][KaSimpleVariableAccessCall] writes to the variable.
     */
    public interface Write : KaSimpleVariableAccess {
        /**
         * A [KtExpression] that represents the new value which is assigned to this variable, or `null` if the assignment is incomplete and
         * lacks the new value.
         */
        public val value: KtExpression?
    }
}
