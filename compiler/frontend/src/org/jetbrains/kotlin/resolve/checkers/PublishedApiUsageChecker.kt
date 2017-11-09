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

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorWithVisibility
import org.jetbrains.kotlin.descriptors.PropertyAccessorDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace

object PublishedApiUsageChecker {
    fun check(
            declaration: KtDeclaration,
            descriptor: DeclarationDescriptor,
            trace: BindingTrace
    ) {
        if (descriptor !is DeclarationDescriptorWithVisibility || descriptor.visibility == Visibilities.INTERNAL) return
        // Don't report the diagnostic twice
        if (descriptor is PropertyAccessorDescriptor) return

        for (entry in declaration.annotationEntries) {
            val annotationDescriptor = trace.get(BindingContext.ANNOTATION, entry) ?: continue
            if (annotationDescriptor.fqName == KotlinBuiltIns.FQ_NAMES.publishedApi) {
                trace.report(Errors.NON_INTERNAL_PUBLISHED_API.on(entry))
            }
        }
    }
}
