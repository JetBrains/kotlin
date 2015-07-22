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

package org.jetbrains.kotlin.load.kotlin

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.load.java.descriptors.JavaClassDescriptor
import org.jetbrains.kotlin.load.java.descriptors.JavaMethodDescriptor
import org.jetbrains.kotlin.resolve.calls.checkers.CallChecker
import org.jetbrains.kotlin.resolve.calls.context.BasicCallResolutionContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.jvm.diagnostics.ErrorsJvm

public class JavaAnnotationMethodCallChecker : CallChecker {
    override fun <F : CallableDescriptor> check(resolvedCall: ResolvedCall<F>, context: BasicCallResolutionContext) {
        val descriptor = resolvedCall.getCandidateDescriptor().getOriginal()
        val containingDeclaration = descriptor.getContainingDeclaration()

        if (containingDeclaration is JavaClassDescriptor &&
            containingDeclaration.getKind() == ClassKind.ANNOTATION_CLASS &&
            descriptor is JavaMethodDescriptor && descriptor.getKind().isReal()
        ) {
            context.trace.report(ErrorsJvm.DEPRECATED_ANNOTATION_METHOD_CALL.on(resolvedCall.getCall().getCallElement()))
        }
    }
}
