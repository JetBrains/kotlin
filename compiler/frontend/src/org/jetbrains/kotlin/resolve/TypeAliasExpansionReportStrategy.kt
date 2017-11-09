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

package org.jetbrains.kotlin.resolve

import org.jetbrains.kotlin.descriptors.TypeAliasDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.types.KotlinType

interface TypeAliasExpansionReportStrategy {
    fun wrongNumberOfTypeArguments(typeAlias: TypeAliasDescriptor, numberOfParameters: Int)
    fun conflictingProjection(typeAlias: TypeAliasDescriptor, typeParameter: TypeParameterDescriptor?, substitutedArgument: KotlinType)
    fun recursiveTypeAlias(typeAlias: TypeAliasDescriptor)
    fun boundsViolationInSubstitution(bound: KotlinType, unsubstitutedArgument: KotlinType, argument: KotlinType, typeParameter: TypeParameterDescriptor)
    fun repeatedAnnotation(annotation: AnnotationDescriptor)

    object DO_NOTHING : TypeAliasExpansionReportStrategy {
        override fun wrongNumberOfTypeArguments(typeAlias: TypeAliasDescriptor, numberOfParameters: Int) {}
        override fun conflictingProjection(typeAlias: TypeAliasDescriptor, typeParameter: TypeParameterDescriptor?, substitutedArgument: KotlinType) {}
        override fun recursiveTypeAlias(typeAlias: TypeAliasDescriptor) {}
        override fun boundsViolationInSubstitution(bound: KotlinType, unsubstitutedArgument: KotlinType, argument: KotlinType, typeParameter: TypeParameterDescriptor) {}
        override fun repeatedAnnotation(annotation: AnnotationDescriptor) {}
    }
}