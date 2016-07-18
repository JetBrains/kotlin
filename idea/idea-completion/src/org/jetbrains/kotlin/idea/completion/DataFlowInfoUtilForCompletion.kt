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

package org.jetbrains.kotlin.idea.completion

import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValue
import org.jetbrains.kotlin.resolve.scopes.receivers.ImplicitReceiver
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.descriptors.PackageViewDescriptor
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValueFactory
import org.jetbrains.kotlin.resolve.calls.smartcasts.IdentifierInfo

fun renderDataFlowValue(value: DataFlowValue): String? {
    // If it is not a stable identifier, there's no point in rendering it
    if (!value.isPredictable) return null

    fun renderId(identifierInfo: IdentifierInfo): String? = with (identifierInfo) {
        when (this) {
            is DataFlowValueFactory.ExpressionIdentifierInfo -> expression.text
            is IdentifierInfo.Receiver -> (this.value as? ImplicitReceiver)?.declarationDescriptor?.name?.let { "this@$it" }
            is IdentifierInfo.Variable -> variable.name.asString()
            is IdentifierInfo.PackageOrClass -> (descriptor as? PackageViewDescriptor)?.let { it.fqName.asString() }
            is IdentifierInfo.Qualified -> renderId(receiverInfo) + "." + renderId(selectorInfo)
            else -> null
        }
    }

    return renderId(value.identifierInfo)
}
