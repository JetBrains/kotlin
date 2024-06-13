/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.resolution

import org.jetbrains.kotlin.psi.KtExpression

public sealed class KaSimpleVariableAccess {
    public object Read : KaSimpleVariableAccess()

    public class Write(
        /**
         * [KtExpression] that represents the new value that should be assigned to this variable. Or null if the assignment is incomplete
         * and misses the new value.
         */
        public val value: KtExpression?,
    ) : KaSimpleVariableAccess()
}
