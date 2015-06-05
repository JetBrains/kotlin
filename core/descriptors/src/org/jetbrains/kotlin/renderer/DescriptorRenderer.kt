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

package org.jetbrains.kotlin.renderer

import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.name.FqNameBase
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.JetType
import org.jetbrains.kotlin.types.TypeProjection

public interface DescriptorRenderer : Renderer<DeclarationDescriptor> {

    public fun renderType(type: JetType): String

    public fun renderTypeArguments(typeArguments: List<TypeProjection>): String

    public fun renderClassifierName(klass: ClassifierDescriptor): String

    public fun renderAnnotation(annotation: AnnotationDescriptor): String

    override fun render(declarationDescriptor: DeclarationDescriptor): String

    public fun renderFunctionParameters(functionDescriptor: FunctionDescriptor): String

    public fun renderName(name: Name): String

    public fun renderFqName(fqName: FqNameBase): String

    public enum class TextFormat {
        PLAIN,
        HTML
    }

    public enum class OverrideRenderingPolicy {
        RENDER_OVERRIDE,
        RENDER_OPEN,
        RENDER_OPEN_OVERRIDE
    }

    public enum class ParameterNameRenderingPolicy {
        ALL,
        ONLY_NON_SYNTHESIZED,
        NONE
    }

    public enum class Modifier {
        VISIBILITY,
        MODALITY,
        OVERRIDE,
        ANNOTATIONS,
        INNER,
        MEMBER_KIND
    }

    /** @see DefaultValueParameterHandler
     */
    public interface ValueParametersHandler {
        public fun appendBeforeValueParameters(function: FunctionDescriptor, stringBuilder: StringBuilder)
        public fun appendAfterValueParameters(function: FunctionDescriptor, stringBuilder: StringBuilder)

        public fun appendBeforeValueParameter(parameter: ValueParameterDescriptor, stringBuilder: StringBuilder)
        public fun appendAfterValueParameter(parameter: ValueParameterDescriptor, stringBuilder: StringBuilder)
    }

    public class DefaultValueParameterHandler : ValueParametersHandler {
        override fun appendBeforeValueParameters(function: FunctionDescriptor, stringBuilder: StringBuilder) {
            stringBuilder.append("(")
        }

        override fun appendAfterValueParameters(function: FunctionDescriptor, stringBuilder: StringBuilder) {
            stringBuilder.append(")")
        }

        override fun appendBeforeValueParameter(parameter: ValueParameterDescriptor, stringBuilder: StringBuilder) {
        }

        override fun appendAfterValueParameter(parameter: ValueParameterDescriptor, stringBuilder: StringBuilder) {
            val function = parameter.getContainingDeclaration() as FunctionDescriptor
            if (parameter.getIndex() != function.getValueParameters().size() - 1) {
                stringBuilder.append(", ")
            }
        }
    }

    companion object {
        public val COMPACT_WITH_MODIFIERS: DescriptorRenderer = DescriptorRendererBuilder()
                .setWithDefinedIn(false)
                .build()

        public val COMPACT: DescriptorRenderer = DescriptorRendererBuilder()
                .setWithDefinedIn(false)
                .setModifiers()
                .build()

        public val COMPACT_WITH_SHORT_TYPES: DescriptorRenderer = DescriptorRendererBuilder()
                .setModifiers()
                .setNameShortness(NameShortness.SHORT)
                .setParameterNameRenderingPolicy(ParameterNameRenderingPolicy.ONLY_NON_SYNTHESIZED)
                .build()

        public val STARTS_FROM_NAME: DescriptorRenderer = DescriptorRendererBuilder()
                .setWithDefinedIn(false)
                .setModifiers()
                .setStartFromName(true)
                .build()

        public val ONLY_NAMES_WITH_SHORT_TYPES: DescriptorRenderer = DescriptorRendererBuilder()
                .setWithDefinedIn(false)
                .setModifiers()
                .setNameShortness(NameShortness.SHORT)
                .setWithoutTypeParameters(true)
                .setParameterNameRenderingPolicy(ParameterNameRenderingPolicy.NONE)
                .setReceiverAfterName(true)
                .setRenderCompanionObjectName(true)
                .setWithoutSuperTypes(true)
                .setStartFromName(true)
                .build()

        public val FQ_NAMES_IN_TYPES: DescriptorRenderer = DescriptorRendererBuilder().build()

        public val SHORT_NAMES_IN_TYPES: DescriptorRenderer = DescriptorRendererBuilder()
                .setNameShortness(NameShortness.SHORT)
                .setParameterNameRenderingPolicy(ParameterNameRenderingPolicy.ONLY_NON_SYNTHESIZED)
                .build()

        public val DEBUG_TEXT: DescriptorRenderer = DescriptorRendererBuilder()
                .setDebugMode(true)
                .setNameShortness(NameShortness.FULLY_QUALIFIED)
                .build()

        public val FLEXIBLE_TYPES_FOR_CODE: DescriptorRenderer = DescriptorRendererBuilder()
                .setFlexibleTypesForCode(true)
                .build()

        public val HTML_COMPACT_WITH_MODIFIERS: DescriptorRenderer = DescriptorRendererBuilder()
                .setWithDefinedIn(false)
                .setTextFormat(TextFormat.HTML)
                .build()

        public val HTML_NAMES_WITH_SHORT_TYPES: DescriptorRenderer = DescriptorRendererBuilder()
                .setWithDefinedIn(false)
                .setNameShortness(NameShortness.SHORT)
                .setRenderCompanionObjectName(true)
                .setTextFormat(TextFormat.HTML)
                .build()

        public val HTML: DescriptorRenderer = DescriptorRendererBuilder()
                .setTextFormat(TextFormat.HTML)
                .build()

        public val HTML_FOR_UNINFERRED_TYPE_PARAMS: DescriptorRenderer = DescriptorRendererBuilder()
                .setUninferredTypeParameterAsName(true)
                .setModifiers()
                .setNameShortness(NameShortness.SHORT)
                .setTextFormat(TextFormat.HTML)
                .build()

        public val DEPRECATION: DescriptorRenderer = DescriptorRendererBuilder()
                .setWithDefinedIn(false)
                .setModifiers()
                .setNameShortness(NameShortness.SHORT)
                .setWithoutTypeParameters(false)
                .setParameterNameRenderingPolicy(ParameterNameRenderingPolicy.NONE)
                .setReceiverAfterName(false)
                .setRenderCompanionObjectName(true)
                .setRenderAccessors(true)
                .setWithoutSuperTypes(true)
                .setRenderDefaultValues(false)
                .setStartFromName(true)
                .build()
    }
}
