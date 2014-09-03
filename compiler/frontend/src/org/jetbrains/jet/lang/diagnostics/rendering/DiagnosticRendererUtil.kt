/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.diagnostics.rendering

import org.jetbrains.jet.renderer.Renderer
import org.jetbrains.jet.lang.descriptors.ClassKind
import org.jetbrains.jet.lang.descriptors.ClassKind.*
import org.jetbrains.jet.lang.descriptors.ClassDescriptor
import org.jetbrains.jet.lang.resolve.DescriptorUtils.isClassObject

public fun <P> renderParameter(parameter: P, renderer: Renderer<P>?): Any = renderer?.render(parameter) ?: parameter

public fun ClassDescriptor.renderKindWithName(): String {
    val kind = getKind().getText()
    if (isClassObject(this)) {
        return "$kind of '${getContainingDeclaration().getName()}'"
    }
    return "$kind '${getName()}'"
}

public fun ClassKind.getText(): String {
    return when (this) {
        CLASS -> "class"
        TRAIT -> "trait"
        ENUM_CLASS -> "enum class"
        ENUM_ENTRY -> "enum entry"
        ANNOTATION_CLASS -> "annotation class"
        OBJECT -> "object"
        CLASS_OBJECT -> "class object"
    }
}