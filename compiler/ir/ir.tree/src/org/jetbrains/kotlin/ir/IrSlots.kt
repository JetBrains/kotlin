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

package org.jetbrains.kotlin.ir

// Slots should be unique within children of particular node type.
// Non-negative numbers are used for slots in indexed container elements, for example, value arguments in a call.
// NB a node can potentially contain several "containers"; it's up to a node how to organize numbering in such case.
// Negative numbers are used for "named" slots, for example, dispatch and extension receivers in a call.

const val DETACHED_SLOT = Int.MIN_VALUE

const val CHILD_EXPRESSION_SLOT = 0
const val ARGUMENT0_SLOT = 0
const val ARGUMENT1_SLOT = 1
const val SETTER_ARGUMENT_INDEX = 0

const val DISPATCH_RECEIVER_SLOT = -1
const val EXTENSION_RECEIVER_SLOT = -2
const val BACKING_FIELD_RECEIVER_SLOT = -3
const val FUNCTION_BODY_SLOT = -4
const val ANONYMOUS_INITIALIZER_BODY_SLOT = -5
const val MODULE_SLOT = -6
const val INITIALIZER_SLOT = -7
const val IF_CONDITION_SLOT = -8
const val IF_THEN_SLOT = -9
const val IF_ELSE_SLOT = -10
const val LOOP_BODY_SLOT = -11
const val LOOP_CONDITION_SLOT = -12
const val TRY_RESULT_SLOT = -13
const val FINALLY_EXPRESSION_SLOT = -14
const val PROPERTY_GETTER_SLOT = -15
const val PROPERTY_SETTER_SLOT = -16
const val DELEGATE_SLOT = -17
const val ENUM_ENTRY_CLASS_SLOT = -18
const val ENUM_ENTRY_INITIALIZER_SLOT = -19