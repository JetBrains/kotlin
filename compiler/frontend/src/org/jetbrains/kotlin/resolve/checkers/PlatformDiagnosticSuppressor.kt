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

import org.jetbrains.kotlin.container.DefaultImplementation
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor

@DefaultImplementation(PlatformDiagnosticSuppressor.Default::class)
interface PlatformDiagnosticSuppressor {
    fun shouldReportUnusedParameter(parameter: VariableDescriptor): Boolean

    fun shouldReportNoBody(descriptor: CallableMemberDescriptor): Boolean

    object Default : PlatformDiagnosticSuppressor {
        override fun shouldReportUnusedParameter(parameter: VariableDescriptor): Boolean = true

        override fun shouldReportNoBody(descriptor: CallableMemberDescriptor): Boolean = true
    }
}
