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

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.resolve.calls.context.BasicCallResolutionContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.inline.InlineUtil
import java.lang.ref.WeakReference

class InlineCheckerWrapper : SimpleCallChecker {
    private var checkersCache: WeakReference<MutableMap<DeclarationDescriptor, SimpleCallChecker>>? = null

    override fun check(resolvedCall: ResolvedCall<*>, context: BasicCallResolutionContext) {
        if (context.isAnnotationContext) return

        var parentDescriptor: DeclarationDescriptor? = context.scope.ownerDescriptor

        while (parentDescriptor != null) {
            if (InlineUtil.isInline(parentDescriptor)) {
                val checker = getChecker(parentDescriptor as SimpleFunctionDescriptor)
                checker.check(resolvedCall, context)
            }

            parentDescriptor = parentDescriptor.containingDeclaration
        }
    }

    private fun getChecker(descriptor: SimpleFunctionDescriptor): SimpleCallChecker {
        val map = checkersCache?.get() ?: hashMapOf()
        checkersCache = checkersCache ?: WeakReference(map)
        return map.getOrPut(descriptor) { InlineChecker(descriptor) }
    }
}
