/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.compiler.plugins.kotlin

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.types.KotlinType

class ValidatedAssignment(
    val validationType: ValidationType,
    val validationCall: ResolvedCall<*>?,
    val uncheckedValidationCall: ResolvedCall<*>?,
    val assignment: ResolvedCall<*>?,
    val assignmentLambda: FunctionDescriptor?, // needed?
    val type: KotlinType,
    val name: String,
    val descriptor: DeclarationDescriptor
)

enum class ValidationType {
    CHANGED,
    SET,
    UPDATE
}