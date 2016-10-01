/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.intentions

import org.jetbrains.kotlin.idea.inspections.IntentionBasedInspection
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtExpression

class ReplaceSizeCheckWithIsNotEmptyInspection : IntentionBasedInspection<KtBinaryExpression>(ReplaceSizeCheckWithIsNotEmptyIntention::class)

class ReplaceSizeCheckWithIsNotEmptyIntention : ReplaceSizeCheckIntention("Replace size check with 'isNotEmpty'") {

    override fun getGenerateMethodSymbol() = "isNotEmpty()"

    override fun getTargetExpression(element: KtBinaryExpression): KtExpression? {
        return when (element.operationToken) {
            KtTokens.EXCLEQ -> when {
                element.right.isZero() -> element.left
                element.left.isZero() -> element.right
                else -> null
            }
            KtTokens.GT -> if (element.right.isZero()) element.left else null
            KtTokens.LT -> if (element.left.isZero()) element.right else null
            KtTokens.GTEQ -> if (element.right.isOne()) element.left else null
            KtTokens.LTEQ -> if (element.left.isOne()) element.right else null
            else -> null
        }
    }
}