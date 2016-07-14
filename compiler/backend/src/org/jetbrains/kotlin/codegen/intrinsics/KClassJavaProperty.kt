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

package org.jetbrains.kotlin.codegen.intrinsics

import org.jetbrains.kotlin.codegen.ExpressionCodegen
import org.jetbrains.kotlin.codegen.StackValue
import org.jetbrains.kotlin.psi.KtClassLiteralExpression
import org.jetbrains.kotlin.resolve.BindingContext.DOUBLE_COLON_LHS
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.org.objectweb.asm.Type

class KClassJavaProperty : IntrinsicPropertyGetter() {
    override fun generate(resolvedCall: ResolvedCall<*>?, codegen: ExpressionCodegen, returnType: Type, receiver: StackValue): StackValue? {
        val receiverValue = resolvedCall!!.extensionReceiver as? ExpressionReceiver ?: return null
        val classLiteralExpression = receiverValue.expression as? KtClassLiteralExpression ?: return null
        val receiverExpression = classLiteralExpression.receiverExpression ?: return null
        val lhs = codegen.bindingContext.get(DOUBLE_COLON_LHS, receiverExpression) ?: return null
        val value = codegen.generateClassLiteralReference(lhs, receiverExpression, /* wrapIntoKClass = */ false)
        return StackValue.coercion(value, returnType)
    }
}
