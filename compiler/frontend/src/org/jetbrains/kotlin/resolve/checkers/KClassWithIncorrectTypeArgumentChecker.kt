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

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.types.UnwrappedType
import org.jetbrains.kotlin.types.typeUtil.builtIns
import org.jetbrains.kotlin.types.typeUtil.contains
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf

object KClassWithIncorrectTypeArgumentChecker : SimpleDeclarationChecker {
    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, diagnosticHolder: DiagnosticSink, bindingContext: BindingContext) {
        if (descriptor !is CallableMemberDescriptor || descriptor.visibility == Visibilities.LOCAL) return

        if (declaration !is KtCallableDeclaration || declaration.typeReference != null) return

        // prevent duplicate reporting
        if (descriptor is PropertyAccessorDescriptor) return

        val returnType = descriptor.returnType ?: return

        var typeParameterWithoutNotNullableUpperBound: TypeParameterDescriptor? = null
        returnType.contains { type ->
            val kClassWithBadArgument = type.isKClassWithBadArgument()
            if (kClassWithBadArgument) {
                type.arguments.singleOrNull()?.type?.constructor?.declarationDescriptor?.let {
                    if (it is TypeParameterDescriptor && it.containingDeclaration == descriptor) {
                        typeParameterWithoutNotNullableUpperBound = it
                    }
                }
            }
            kClassWithBadArgument
        }

        if (typeParameterWithoutNotNullableUpperBound != null) {
            diagnosticHolder.report(Errors.KCLASS_WITH_NULLABLE_TYPE_PARAMETER_IN_SIGNATURE.on(declaration, typeParameterWithoutNotNullableUpperBound!!))
        }
    }

    private fun UnwrappedType.isKClassWithBadArgument(): Boolean {
        val argumentType = arguments.singleOrNull()?.let { if (it.isStarProjection) null else it.type.unwrap() } ?: return false
        val klass = constructor.declarationDescriptor as? ClassDescriptor ?: return false

        return KotlinBuiltIns.isKClass(klass) && !argumentType.isSubtypeOf(argumentType.builtIns.anyType)
    }
}
