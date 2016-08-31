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

const val DETACHED_SLOT = Int.MIN_VALUE
const val CHILD_EXPRESSION_SLOT = 0
const val ARGUMENT0_SLOT = 0
const val ARGUMENT1_SLOT = 1
const val DISPATCH_RECEIVER_SLOT = -1
const val EXTENSION_RECEIVER_SLOT = -2
const val FUNCTION_BODY_SLOT = 0
const val MODULE_SLOT = 0
const val INITIALIZER_SLOT = 0
const val IF_CONDITION_SLOT = 0
const val IF_THEN_SLOT = 1
const val IF_ELSE_SLOT = -1
const val LOOP_BODY_SLOT = -1
const val LOOP_CONDITION_SLOT = -2
const val SETTER_ARGUMENT_INDEX = 0
const val TRY_RESULT_SLOT = -1
const val FINALLY_EXPRESSION_SLOT = -2
const val NESTED_INITIALIZERS_SLOT = -1
const val PROPERTY_GETTER_SLOT = -1
const val PROPERTY_SETTER_SLOT = -2