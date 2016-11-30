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

package org.jetbrains.kotlin.backend.common

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns

val SUSPEND_WITH_CURRENT_CONTINUATION_NAME = Name.identifier("suspendWithCurrentContinuation")

fun FunctionDescriptor.getBuiltInSuspendWithCurrentContinuation() =
            builtIns.builtInsPackageFragments.singleOrNull { it.fqName == KotlinBuiltIns.COROUTINES_PACKAGE_FQ_NAME }
                    ?.getMemberScope()
                    ?.getContributedFunctions(SUSPEND_WITH_CURRENT_CONTINUATION_NAME, NoLookupLocation.FROM_BACKEND)
                    ?.singleOrNull()


