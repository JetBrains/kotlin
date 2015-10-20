/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.psi

import com.intellij.lang.ASTNode

public class KtFunctionLiteralArgument(node: ASTNode) : KtValueArgument(node), FunctionLiteralArgument {

    override fun getArgumentExpression() = super<KtValueArgument>.getArgumentExpression()!!

    override fun getFunctionLiteral(): KtFunctionLiteralExpression = getArgumentExpression().unpackFunctionLiteral()!!
}

public fun KtExpression.unpackFunctionLiteral(): KtFunctionLiteralExpression? {
    return when (this) {
        is KtFunctionLiteralExpression -> this
        is KtLabeledExpression -> getBaseExpression()?.unpackFunctionLiteral()
        is KtAnnotatedExpression -> getBaseExpression()?.unpackFunctionLiteral()
        else -> null
    }
}


