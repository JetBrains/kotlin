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
import org.jetbrains.kotlin.resolve.scopes.receivers.ThisReceiver
import org.jetbrains.kotlin.psi.JetExpression
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.descriptors.PackageViewDescriptor

fun renderDataFlowValue(value: DataFlowValue): String? {
    // If it is not a stable identifier, there's no point in rendering it
    if (!value.isPredictable()) return null

    fun renderId(id: Any?): String? {
        return when (id) {
            is JetExpression -> id.getText()
            is ThisReceiver -> "this@${id.getDeclarationDescriptor().getName()}"
            is VariableDescriptor -> id.getName().asString()
            is PackageViewDescriptor -> id.fqName.asString()
            is com.intellij.openapi.util.Pair<*, *> -> renderId(id.first) + "." + renderId(id.second)
            else -> null
        }
    }
    return renderId(value.getId())
}
