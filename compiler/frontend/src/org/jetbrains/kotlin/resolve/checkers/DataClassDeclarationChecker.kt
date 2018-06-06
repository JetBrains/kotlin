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

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclaration

class DataClassDeclarationChecker : DeclarationChecker {
    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        if (descriptor !is ClassDescriptor) return
        if (declaration !is KtClassOrObject) return

        if (descriptor.isData) {
            if (descriptor.unsubstitutedPrimaryConstructor == null && descriptor.constructors.isNotEmpty()) {
                declaration.nameIdentifier?.let { context.trace.report(Errors.PRIMARY_CONSTRUCTOR_REQUIRED_FOR_DATA_CLASS.on(it)) }
            }
            val primaryConstructor = declaration.primaryConstructor
            val parameters = primaryConstructor?.valueParameters ?: emptyList()
            if (parameters.isEmpty()) {
                (primaryConstructor?.valueParameterList ?: declaration.nameIdentifier)?.let {
                    context.trace.report(Errors.DATA_CLASS_WITHOUT_PARAMETERS.on(it))
                }
            }
            for (parameter in parameters) {
                if (parameter.isVarArg) {
                    context.trace.report(Errors.DATA_CLASS_VARARG_PARAMETER.on(parameter))
                }
                if (!parameter.hasValOrVar()) {
                    context.trace.report(Errors.DATA_CLASS_NOT_PROPERTY_PARAMETER.on(parameter))
                }
            }
        }
    }
}
