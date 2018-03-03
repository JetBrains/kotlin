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

package org.jetbrains.kotlin.backend.js.translate.context

import org.jetbrains.kotlin.js.backend.ast.JsExpression
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.psi.KtExpression

import java.util.Collections

class AliasingContext private constructor(
    private val parent: AliasingContext?,
    private val aliasesForDescriptors: Map<DeclarationDescriptor, JsExpression>?,
    private val aliasesForExpressions: Map<KtExpression, JsExpression>?
) {

    fun inner(): AliasingContext {
        return AliasingContext(this, null, null)
    }

    fun inner(descriptor: DeclarationDescriptor, alias: JsExpression): AliasingContext {
        return AliasingContext(this, Collections.singletonMap(descriptor, alias), null)
    }

    fun withExpressionsAliased(aliasesForExpressions: Map<KtExpression, JsExpression>): AliasingContext {
        return AliasingContext(this, null, aliasesForExpressions)
    }

    fun withDescriptorsAliased(aliases: Map<DeclarationDescriptor, JsExpression>): AliasingContext {
        return AliasingContext(this, aliases, null)
    }

    fun getAliasForDescriptor(descriptor: DeclarationDescriptor): JsExpression? {
        // these aliases cannot be shared and applicable only in current context
        val alias = if (aliasesForDescriptors != null) aliasesForDescriptors[descriptor.original] else null
        val result = if (alias != null || parent == null) alias else parent.getAliasForDescriptor(descriptor)
        return result?.deepCopy()
    }

    fun getAliasForExpression(element: KtExpression): JsExpression? {
        val alias = if (aliasesForExpressions != null) aliasesForExpressions[element] else null
        val result = if (alias != null || parent == null) alias else parent.getAliasForExpression(element)
        return result?.deepCopy()
    }

    companion object {
        @JvmStatic
        val cleanContext: AliasingContext
            get() = AliasingContext(null, null, null)
    }
}
