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

package org.jetbrains.kotlin.codegen.coroutines

import org.jetbrains.kotlin.coroutines.CONTINUATION_INTERFACE_FQ_NAME
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.org.objectweb.asm.Type

val CONTINUATION_INTERFACE_ASM_TYPE = Type.getObjectType(JvmClassName.byFqNameWithoutInnerClasses(CONTINUATION_INTERFACE_FQ_NAME).internalName)

// These classes do not actually exist at runtime
val CONTINUATION_METHOD_ANNOTATION_DESC = "Lkotlin/ContinuationMethod;"

const val SUSPENSION_POINT_MARKER_OWNER = "kotlin/Markers"
const val SUSPENSION_POINT_MARKER_NAME = "suspensionPoint"

const val COROUTINE_CONTROLLER_FIELD_NAME = "controller"
const val COROUTINE_LABEL_FIELD_NAME = "label"
