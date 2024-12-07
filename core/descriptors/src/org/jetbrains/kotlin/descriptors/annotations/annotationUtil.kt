/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.descriptors.annotations

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.constants.AnnotationValue
import org.jetbrains.kotlin.resolve.constants.ArrayValue
import org.jetbrains.kotlin.resolve.constants.EnumValue
import org.jetbrains.kotlin.resolve.constants.StringValue
import org.jetbrains.kotlin.types.Variance

fun KotlinBuiltIns.createDeprecatedAnnotation(
        message: String,
        replaceWith: String = "",
        level: String = "WARNING",
        forcePropagationDeprecationToOverrides: Boolean = false,
): AnnotationDescriptor {
    val replaceWithAnnotation = BuiltInAnnotationDescriptor(
        this,
        StandardNames.FqNames.replaceWith,
        mapOf(
                    REPLACE_WITH_EXPRESSION_NAME to StringValue(replaceWith),
                    REPLACE_WITH_IMPORTS_NAME to ArrayValue(emptyList()) { module ->
                        module.builtIns.getArrayType(Variance.INVARIANT, stringType)
                    }
            )
    )

    return BuiltInAnnotationDescriptor(
        this,
        StandardNames.FqNames.deprecated,
        mapOf(
                    DEPRECATED_MESSAGE_NAME to StringValue(message),
                    DEPRECATED_REPLACE_WITH_NAME to AnnotationValue(replaceWithAnnotation),
                    DEPRECATED_LEVEL_NAME to EnumValue(
                        ClassId.topLevel(StandardNames.FqNames.deprecationLevel),
                        Name.identifier(level)
                    )
            ),
        forcePropagationDeprecationToOverrides,
    )
}

private val DEPRECATED_MESSAGE_NAME = Name.identifier("message")
private val DEPRECATED_REPLACE_WITH_NAME = Name.identifier("replaceWith")
private val DEPRECATED_LEVEL_NAME = Name.identifier("level")
private val REPLACE_WITH_EXPRESSION_NAME = Name.identifier("expression")
private val REPLACE_WITH_IMPORTS_NAME = Name.identifier("imports")
