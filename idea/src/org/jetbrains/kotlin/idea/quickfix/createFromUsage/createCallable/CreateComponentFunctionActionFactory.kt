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

package org.jetbrains.kotlin.idea.quickfix.createFromUsage.createCallable

import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.idea.core.quickfix.QuickFixUtil
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.CallableInfo
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.FunctionInfo
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.TypeInfo
import org.jetbrains.kotlin.psi.KtDestructuringDeclaration
import org.jetbrains.kotlin.psi.KtForExpression
import org.jetbrains.kotlin.resolve.DataClassDescriptorResolver
import org.jetbrains.kotlin.types.Variance

object CreateComponentFunctionActionFactory : CreateCallableMemberFromUsageFactory<KtDestructuringDeclaration>() {
    override fun getElementOfInterest(diagnostic: Diagnostic): KtDestructuringDeclaration? {
        QuickFixUtil.getParentElementOfType(diagnostic, KtDestructuringDeclaration::class.java)?.let { return it }
        return QuickFixUtil.getParentElementOfType(diagnostic, KtForExpression::class.java)?.destructuringDeclaration
    }

    override fun createCallableInfo(element: KtDestructuringDeclaration, diagnostic: Diagnostic): CallableInfo? {
        val diagnosticWithParameters = Errors.COMPONENT_FUNCTION_MISSING.cast(diagnostic)

        val name = diagnosticWithParameters.a
        if (!DataClassDescriptorResolver.isComponentLike(name)) return null

        val componentNumber = DataClassDescriptorResolver.getComponentIndex(name.asString()) - 1

        val ownerType = element.initializer?.let { TypeInfo(it, Variance.IN_VARIANCE) }
                        ?: TypeInfo(diagnosticWithParameters.b, Variance.IN_VARIANCE)
        val entries = element.entries

        val entry = entries[componentNumber]
        val returnType = TypeInfo(entry, Variance.OUT_VARIANCE)

        return FunctionInfo(name.identifier, ownerType, returnType, isOperator = true)
    }
}
