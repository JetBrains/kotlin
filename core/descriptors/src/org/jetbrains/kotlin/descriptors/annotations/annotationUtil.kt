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
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.constants.AnnotationValue
import org.jetbrains.kotlin.resolve.constants.ArrayValue
import org.jetbrains.kotlin.resolve.constants.StringValue
import org.jetbrains.kotlin.types.Variance

public fun KotlinBuiltIns.createDeprecatedAnnotation(message: String, replaceWith: String): AnnotationDescriptor {
    val deprecatedAnnotation = deprecatedAnnotation
    val parameters = deprecatedAnnotation.unsubstitutedPrimaryConstructor!!.valueParameters

    val replaceWithClass = getBuiltInClassByName(Name.identifier("ReplaceWith"))

    val replaceWithParameters = replaceWithClass.unsubstitutedPrimaryConstructor!!.valueParameters
    return AnnotationDescriptorImpl(
            deprecatedAnnotation.defaultType,
            mapOf(
                    parameters["message"] to StringValue(message, this),
                    parameters["replaceWith"] to AnnotationValue(
                            AnnotationDescriptorImpl(
                                    replaceWithClass.defaultType,
                                    mapOf(
                                            replaceWithParameters["expression"] to StringValue(replaceWith, this),
                                            replaceWithParameters["imports"]    to ArrayValue(
                                                    emptyList(), getArrayType(Variance.INVARIANT, stringType), this)
                                    ),
                                    SourceElement.NO_SOURCE
                            )
                    )
            ),
            SourceElement.NO_SOURCE)
}

private operator fun Collection<ValueParameterDescriptor>.get(parameterName: String) = single { it.name.asString() == parameterName }
