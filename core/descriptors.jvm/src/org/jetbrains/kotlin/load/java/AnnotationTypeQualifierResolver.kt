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

package org.jetbrains.kotlin.load.java

import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.constants.ArrayValue
import org.jetbrains.kotlin.resolve.constants.ConstantValue
import org.jetbrains.kotlin.resolve.constants.EnumValue
import org.jetbrains.kotlin.resolve.descriptorUtil.annotationClass

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
class AnnotationTypeQualifierResolver(javaTypeEnhancementState: JavaTypeEnhancementState) :
    AbstractAnnotationTypeQualifierResolver<AnnotationDescriptor>(javaTypeEnhancementState) {

    override val isK2: Boolean
        get() = false

    override val AnnotationDescriptor.metaAnnotations: Iterable<AnnotationDescriptor>
        get() = annotationClass?.annotations ?: emptyList()

    override val AnnotationDescriptor.key: Any
        get() = annotationClass!!

    override val AnnotationDescriptor.fqName: FqName?
        get() = fqName

    override fun AnnotationDescriptor.enumArguments(onlyValue: Boolean): Iterable<String> =
        allValueArguments.flatMap { (parameter, argument) ->
            if (!onlyValue || parameter == JvmAnnotationNames.DEFAULT_ANNOTATION_MEMBER_NAME)
                argument.toEnumNames()
            else
                emptyList()
        }

    private fun ConstantValue<*>.toEnumNames(): List<String> =
        when (this) {
            is ArrayValue -> value.flatMap { it.toEnumNames() }
            is EnumValue -> listOf(enumEntryName.identifier)
            else -> emptyList()
        }
}
