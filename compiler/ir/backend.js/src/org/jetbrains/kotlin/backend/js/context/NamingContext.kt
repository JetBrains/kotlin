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

package org.jetbrains.kotlin.backend.js.context

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.js.backend.ast.JsName
import org.jetbrains.kotlin.js.backend.ast.JsNameRef

interface NamingContext {
    val tags: Provider<DeclarationDescriptor, String?>

    val innerNames: Provider<DeclarationDescriptor, JsName>

    val names: Provider<DeclarationDescriptor, JsName>

    val qualifiedReferences: Provider<DeclarationDescriptor, JsNameRef>

    val objectInnerNames: Provider<ClassDescriptor, JsName>

    val backingFieldNames: Provider<PropertyDescriptor, JsName>
}