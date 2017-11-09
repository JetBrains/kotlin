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

package org.jetbrains.kotlin.resolve.jvm.checkers

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.load.java.descriptors.JavaClassDescriptor
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.resolve.calls.checkers.AdditionalTypeChecker
import org.jetbrains.kotlin.resolve.calls.context.ResolutionContext
import org.jetbrains.kotlin.types.KotlinType


class JavaTypeAccessibilityChecker : AdditionalTypeChecker {
    override fun checkType(
            expression: KtExpression,
            expressionType: KotlinType,
            expressionTypeWithSmartCast: KotlinType,
            c: ResolutionContext<*>
    ) {
        // NB in Kotlin class hierarchy leading to "pathological" type inference is impossible
        // due to EXPOSED_SUPER_CLASS & EXPOSED_SUPER_INTERFACE checks.
        // To avoid superfluous diagnostics in case of invisible member class and so on,
        // we consider only Java classes as possibly inaccessible.

        if (c.isDebuggerContext) return

        val inaccessibleClasses = findInaccessibleJavaClasses(expressionType, c)
        if (inaccessibleClasses.isNotEmpty()) {
            c.trace.report(Errors.INACCESSIBLE_TYPE.on(expression, expressionType, inaccessibleClasses))
            return
        }

        if (expressionTypeWithSmartCast != expressionType) {
            val inaccessibleClassesWithSmartCast = findInaccessibleJavaClasses(expressionTypeWithSmartCast, c)
            if (inaccessibleClassesWithSmartCast.isNotEmpty()) {
                c.trace.report(Errors.INACCESSIBLE_TYPE.on(expression, expressionType, inaccessibleClassesWithSmartCast))
            }
        }
    }

    private fun findInaccessibleJavaClasses(type: KotlinType, c: ResolutionContext<*>): Collection<ClassDescriptor> {
        val scopeOwner = c.scope.ownerDescriptor
        val inaccessibleJavaClasses = LinkedHashSet<ClassDescriptor>()
        findInaccessibleJavaClassesRec(type, scopeOwner, inaccessibleJavaClasses)
        return inaccessibleJavaClasses
    }

    private fun findInaccessibleJavaClassesRec(
            type: KotlinType,
            scopeOwner: DeclarationDescriptor,
            inaccessibleClasses: MutableCollection<ClassDescriptor>
    ) {
        val declarationDescriptor = type.constructor.declarationDescriptor

        if (declarationDescriptor is JavaClassDescriptor) {
            if (!Visibilities.isVisibleIgnoringReceiver(declarationDescriptor, scopeOwner)) {
                inaccessibleClasses.add(declarationDescriptor)
            }
        }

        for (typeProjection in type.arguments) {
            if (typeProjection.isStarProjection) continue
            findInaccessibleJavaClassesRec(typeProjection.type, scopeOwner, inaccessibleClasses)
        }
    }

}