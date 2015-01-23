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

package org.jetbrains.kotlin.resolve.calls.checkers

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.context.BasicCallResolutionContext
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import java.lang.ref.WeakReference

public class InlineCheckerWrapper : CallChecker {
    private var checkersCache: WeakReference<MutableMap<DeclarationDescriptor, CallChecker>>? = null

    override fun <F : CallableDescriptor?> check(resolvedCall: ResolvedCall<F>, context: BasicCallResolutionContext) {
        if (context.isAnnotationContext) return

        var parentDescriptor: DeclarationDescriptor? = context.scope.getContainingDeclaration()

        while (parentDescriptor != null) {
            val descriptor = parentDescriptor!!

            if (descriptor is SimpleFunctionDescriptor && descriptor.getInlineStrategy().isInline()) {
                val checker = getChecker(descriptor)
                checker.check(resolvedCall, context)
            }

            parentDescriptor = parentDescriptor?.getContainingDeclaration()
        }
    }

    private fun getChecker(descriptor: SimpleFunctionDescriptor): CallChecker {
        val map = checkersCache?.get() ?: hashMapOf()
        checkersCache = checkersCache ?: WeakReference(map)
        return map.getOrPut(descriptor) { InlineChecker(descriptor) }
    }
}