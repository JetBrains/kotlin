/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.ProblemsHolder
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.search.usagesSearch.descriptor
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.namedFunctionVisitor
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.typeUtil.isBoolean
import org.jetbrains.kotlin.types.typeUtil.isNullableAny
import org.jetbrains.kotlin.util.OperatorNameConventions

class KotlinCovariantEqualsInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) = namedFunctionVisitor(fun(function) {
        if (function.isTopLevel || function.isLocal) return
        if (function.nameAsName != OperatorNameConventions.EQUALS) return
        val nameIdentifier = function.nameIdentifier ?: return
        val classOrObject = function.containingClassOrObject ?: return
        if (classOrObject is KtObjectDeclaration && classOrObject.isCompanion()) return

        val parameter = function.valueParameters.singleOrNull() ?: return
        val typeReference = parameter.typeReference ?: return
        val type = parameter.analyze(BodyResolveMode.PARTIAL)[BindingContext.TYPE, typeReference] ?: return
        if (KotlinBuiltIns.isNullableAny(type)) return

        if (classOrObject.body?.children?.any { (it as? KtNamedFunction)?.isEquals() == true } == true) return

        holder.registerProblem(nameIdentifier, "'equals' should take 'Any?' as its argument")
    })

    private fun KtNamedFunction.isEquals(): Boolean {
        if (!hasModifier(KtTokens.OVERRIDE_KEYWORD)) return false
        if (nameAsName != OperatorNameConventions.EQUALS) return false
        val descriptor = this.descriptor as? FunctionDescriptor ?: return false
        if (descriptor.valueParameters.singleOrNull()?.type?.isNullableAny() != true) return false
        if (descriptor.returnType?.isBoolean() != true) return false
        return true
    }
}