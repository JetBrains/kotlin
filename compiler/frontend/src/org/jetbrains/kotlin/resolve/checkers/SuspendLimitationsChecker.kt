/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.resolve.checkers

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.abbreviationFqName
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameOrNull
import org.jetbrains.kotlin.types.getAbbreviation
import org.jetbrains.kotlin.util.OperatorNameConventions

object SuspendLimitationsChecker : DeclarationChecker {
    private val UNSUPPORTED_OPERATOR_NAMES = setOf(
        OperatorNameConventions.CONTAINS,
        OperatorNameConventions.GET, OperatorNameConventions.SET,
        OperatorNameConventions.PROVIDE_DELEGATE, OperatorNameConventions.GET_VALUE, OperatorNameConventions.SET_VALUE
    )

    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        if (descriptor !is FunctionDescriptor || !descriptor.isSuspend) return

        if (descriptor.isOperator && descriptor.name in UNSUPPORTED_OPERATOR_NAMES) {
            declaration.modifierList?.getModifier(KtTokens.OPERATOR_KEYWORD)?.let {
                context.trace.report(Errors.UNSUPPORTED.on(it, "suspend operator \"${descriptor.name}\""))
            }
        }

        if (descriptor.annotations.any(AnnotationDescriptor::isKotlinTestAnnotation)) {
            declaration.modifierList?.getModifier(KtTokens.SUSPEND_KEYWORD)?.let {
                context.trace.report(Errors.UNSUPPORTED.on(it, "suspend test functions"))
            }
        }
    }
}

private val KOTLIN_TEST_TEST_FQNAME = FqName("kotlin.test.Test")
private fun AnnotationDescriptor.isKotlinTestAnnotation() =
    fqName == KOTLIN_TEST_TEST_FQNAME || abbreviationFqName == KOTLIN_TEST_TEST_FQNAME
