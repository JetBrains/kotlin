/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix.createFromUsage.createCallable

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.idea.core.quickfix.QuickFixUtil
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.CallableInfo
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.FunctionInfo
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.TypeInfo
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtDestructuringDeclaration
import org.jetbrains.kotlin.psi.KtForExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
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

        val targetType = diagnosticWithParameters.b
        val targetClassDescriptor = targetType.constructor.declarationDescriptor as? ClassDescriptor
        if (targetClassDescriptor != null && targetClassDescriptor.isData) return null

        val ownerTypeInfo = TypeInfo(targetType, Variance.IN_VARIANCE)
        val entries = element.entries

        val entry = entries[componentNumber]
        val returnTypeInfo = TypeInfo(entry, Variance.OUT_VARIANCE)

        return FunctionInfo(
            name.identifier,
            ownerTypeInfo,
            returnTypeInfo,
            modifierList = KtPsiFactory(element).createModifierList(KtTokens.OPERATOR_KEYWORD)
        )
    }
}
