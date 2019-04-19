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

package org.jetbrains.kotlin.resolve.jvm

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.util.slicedMap.BasicWritableSlice
import org.jetbrains.kotlin.util.slicedMap.RewritePolicy
import org.jetbrains.kotlin.util.slicedMap.Slices
import org.jetbrains.kotlin.util.slicedMap.WritableSlice

object JvmBindingContextSlices {
    @JvmField
    val RUNTIME_ASSERTION_INFO: WritableSlice<KtExpression, RuntimeAssertionInfo> = BasicWritableSlice(RewritePolicy.DO_NOTHING)

    @JvmField
    val RECEIVER_RUNTIME_ASSERTION_INFO: WritableSlice<ExpressionReceiver, RuntimeAssertionInfo> = BasicWritableSlice(RewritePolicy.DO_NOTHING)

    @JvmField
    val BODY_RUNTIME_ASSERTION_INFO: WritableSlice<KtExpression, RuntimeAssertionInfo> = BasicWritableSlice(RewritePolicy.DO_NOTHING)

    @JvmField
    val LOAD_FROM_JAVA_SIGNATURE_ERRORS: WritableSlice<DeclarationDescriptor, List<String>> = Slices.createCollectiveSlice()

    @JvmField
    val RUNTIME_ASSERTION_INFO_ON_GENERIC_CALL: WritableSlice<KtElement, RuntimeAssertionInfo> =
        BasicWritableSlice(RewritePolicy.DO_NOTHING)

    @JvmField
    val RUNTIME_ASSERTION_INFO_ON_DELEGATES: WritableSlice<KtElement, RuntimeAssertionInfo> =
        BasicWritableSlice(RewritePolicy.DO_NOTHING)

    init {
        BasicWritableSlice.initSliceDebugNames(JvmBindingContextSlices::class.java)
    }
}
