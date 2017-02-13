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

package org.jetbrains.kotlin.resolve

import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtModifierList
import org.jetbrains.kotlin.resolve.calls.model.ResolvedValueArgument
import org.jetbrains.kotlin.resolve.calls.results.OverloadResolutionResults
import org.jetbrains.kotlin.resolve.constants.ConstantValue
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.types.KotlinType

abstract class AnnotationResolver {
    fun resolveAnnotationsWithoutArguments(
            scope: LexicalScope,
            modifierList: KtModifierList?,
            trace: BindingTrace
    ): Annotations = resolveAnnotationsFromModifierList(scope, modifierList, trace, false)

    fun resolveAnnotationsWithArguments(
            scope: LexicalScope,
            modifierList: KtModifierList?,
            trace: BindingTrace
    ): Annotations = resolveAnnotationsFromModifierList(scope, modifierList, trace, true)


    private fun resolveAnnotationsFromModifierList(
            scope: LexicalScope,
            modifierList: KtModifierList?,
            trace: BindingTrace,
            shouldResolveArguments: Boolean
    ): Annotations {
        if (modifierList == null) {
            return Annotations.EMPTY
        }

        return resolveAnnotationEntries(scope, modifierList.annotationEntries, trace, shouldResolveArguments)
    }

    fun resolveAnnotationsWithoutArguments(
            scope: LexicalScope,
            annotationEntries: @JvmSuppressWildcards List<KtAnnotationEntry>,
            trace: BindingTrace
    ): Annotations = resolveAnnotationEntries(scope, annotationEntries, trace, false)

    fun resolveAnnotationsWithArguments(
            scope: LexicalScope,
            annotationEntries: @JvmSuppressWildcards List<KtAnnotationEntry>,
            trace: BindingTrace
    ): Annotations = resolveAnnotationEntries(scope, annotationEntries, trace, true)

    protected abstract fun resolveAnnotationEntries(
            scope: LexicalScope,
            annotationEntries: @JvmSuppressWildcards List<KtAnnotationEntry>,
            trace: BindingTrace,
            shouldResolveArguments: Boolean
    ): Annotations


    abstract fun resolveAnnotationType(scope: LexicalScope, entryElement: KtAnnotationEntry, trace: BindingTrace): KotlinType

    abstract fun resolveAnnotationCall(
            annotationEntry: KtAnnotationEntry,
            scope: LexicalScope,
            trace: BindingTrace
    ): OverloadResolutionResults<FunctionDescriptor>

    abstract fun getAnnotationArgumentValue(
            trace: BindingTrace,
            valueParameter: ValueParameterDescriptor,
            resolvedArgument: ResolvedValueArgument
    ): ConstantValue<*>?
}
