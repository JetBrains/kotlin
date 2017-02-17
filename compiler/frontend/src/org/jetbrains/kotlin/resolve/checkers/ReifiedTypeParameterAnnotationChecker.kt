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

package org.jetbrains.kotlin.resolve.checkers

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtTypeParameter
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.inline.InlineUtil

class ReifiedTypeParameterAnnotationChecker : SimpleDeclarationChecker {

    override fun check(
            declaration: KtDeclaration,
            descriptor: DeclarationDescriptor,
            diagnosticHolder: DiagnosticSink,
            bindingContext: BindingContext
    ) {
        if (descriptor is CallableDescriptor &&
            !(InlineUtil.isInline(descriptor) || InlineUtil.isPropertyWithAllAccessorsAreInline(descriptor))) {
            checkTypeParameterDescriptorsAreNotReified(descriptor.typeParameters, diagnosticHolder)
        }

        if (descriptor is ClassDescriptor) {
            checkTypeParameterDescriptorsAreNotReified(descriptor.declaredTypeParameters, diagnosticHolder)
        }
    }


    private fun checkTypeParameterDescriptorsAreNotReified(
            typeParameterDescriptors: List<TypeParameterDescriptor>,
            diagnosticHolder: DiagnosticSink
    ) {
        for (reifiedTypeParameterDescriptor in typeParameterDescriptors.filter { it.isReified }) {
            val typeParameterDeclaration = DescriptorToSourceUtils.descriptorToDeclaration(reifiedTypeParameterDescriptor)
            if (typeParameterDeclaration !is KtTypeParameter) throw AssertionError("JetTypeParameter expected")

            diagnosticHolder.report(
                    Errors.REIFIED_TYPE_PARAMETER_NO_INLINE.on(
                            typeParameterDeclaration.modifierList!!.getModifier(KtTokens.REIFIED_KEYWORD)!!
                    )
            )
        }
    }
}
