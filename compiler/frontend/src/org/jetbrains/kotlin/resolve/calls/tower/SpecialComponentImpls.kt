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

package org.jetbrains.kotlin.resolve.calls.tower

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.calls.components.CommonSupertypeCalculator
import org.jetbrains.kotlin.resolve.calls.components.IsDescriptorFromSourcePredicate
import org.jetbrains.kotlin.types.CommonSupertypes
import org.jetbrains.kotlin.types.UnwrappedType

object CommonSupertypeCalculatorImpl : CommonSupertypeCalculator {
    override fun invoke(p1: Collection<UnwrappedType>): UnwrappedType = CommonSupertypes.commonSupertype(p1).unwrap()
}

object IsDescriptorFromSourcePredicateImpl: IsDescriptorFromSourcePredicate {
    override fun invoke(p1: CallableDescriptor) = DescriptorToSourceUtils.descriptorToDeclaration(p1) != null
}