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

package org.jetbrains.kotlin.resolve.calls

import com.intellij.lang.ASTNode
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtQualifiedExpression
import java.util.*

fun unrollToLeftMostQualifiedExpression(expression: KtQualifiedExpression): List<KtQualifiedExpression> {
    val unrolled = arrayListOf<KtQualifiedExpression>()

    var finger = expression
    while (true) {
        unrolled.add(finger)
        val receiver = finger.receiverExpression
        if (receiver !is KtQualifiedExpression) {
            break
        }
        finger = receiver
    }

    return unrolled.asReversed()
}

data class CallExpressionElement internal constructor (val qualified: KtQualifiedExpression) {

    val receiver: KtExpression
        get() = qualified.receiverExpression

    val selector: KtExpression?
        get() = qualified.selectorExpression

    val safe: Boolean
        get() = qualified.operationSign == KtTokens.SAFE_ACCESS

    val node: ASTNode
        get() = qualified.operationTokenNode
}