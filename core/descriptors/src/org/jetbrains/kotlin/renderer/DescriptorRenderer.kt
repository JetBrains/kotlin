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

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.FqNameBase
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.JetType
import org.jetbrains.kotlin.types.TypeConstructor
import org.jetbrains.kotlin.types.TypeProjection

public abstract class DescriptorRenderer : Renderer<DeclarationDescriptor> {
    public fun withOptions(changeOptions: DescriptorRendererOptions.() -> Unit): DescriptorRenderer {
        val options = (this as DescriptorRendererImpl).options.copy()
        options.changeOptions()
        options.lock()
        return DescriptorRendererImpl(options)
    }

    public abstract fun renderType(type: JetType): String

    public abstract fun renderTypeArguments(typeArguments: List<TypeProjection>): String

    public abstract fun renderTypeProjection(typeProjection: TypeProjection): String

    public abstract fun renderTypeConstructor(typeConstructor: TypeConstructor): String

    public abstract fun renderClassifierName(klass: ClassifierDescriptor): String

    public abstract fun renderAnnotation(annotation: AnnotationDescriptor, target: AnnotationUseSiteTarget? = null): String

    override abstract fun render(declarationDescriptor: DeclarationDescriptor): String

    public abstract fun renderValueParameters(parameters: Collection<ValueParameterDescriptor>, synthesizedParameterNames: Boolean): String

    public fun renderFunctionParameters(functionDescriptor: FunctionDescriptor): String
            = renderValueParameters(functionDescriptor.valueParameters, functionDescriptor.hasSynthesizedParameterNames())

    public abstract fun renderName(name: Name): String

    public abstract fun renderFqName(fqName: FqNameBase): String

    public interface ValueParametersHandler {
        public fun appendBeforeValueParameters(parameterCount: Int, builder: StringBuilder)
        public fun appendAfterValueParameters(parameterCount: Int, builder: StringBuilder)

        public fun appendBeforeValueParameter(parameter: ValueParameterDescriptor, parameterIndex: Int, parameterCount: Int, builder: StringBuilder)
        public fun appendAfterValueParameter(parameter: ValueParameterDescriptor, parameterIndex: Int, parameterCount: Int, builder: StringBuilder)

        public object DEFAULT : ValueParametersHandler {
            override fun appendBeforeValueParameters(parameterCount: Int, builder: StringBuilder) {
                builder.append("(")
            }

            override fun appendAfterValueParameters(parameterCount: Int, builder: StringBuilder) {
                builder.append(")")
            }

            override fun appendBeforeValueParameter(parameter: ValueParameterDescriptor, parameterIndex: Int, parameterCount: Int, builder: StringBuilder) {
            }

            override fun appendAfterValueParameter(parameter: ValueParameterDescriptor, parameterIndex: Int, parameterCount: Int, builder: StringBuilder) {
                if (parameterIndex != parameterCount - 1) {
                    builder.append(", ")
                }
            }
        }
    }

    companion object {
        public fun withOptions(changeOptions: DescriptorRendererOptions.() -> Unit): DescriptorRenderer {
            val options = DescriptorRendererOptionsImpl()
            options.changeOptions()
            options.lock()
            return DescriptorRendererImpl(options)
        }

        public val COMPACT_WITH_MODIFIERS: DescriptorRenderer = withOptions {
            withDefinedIn = false
        }

        public val COMPACT: DescriptorRenderer = withOptions {
            withDefinedIn = false
            modifiers = emptySet()
        }

        public val COMPACT_WITH_SHORT_TYPES: DescriptorRenderer = withOptions {
            modifiers = emptySet()
            nameShortness = NameShortness.SHORT
            parameterNameRenderingPolicy = ParameterNameRenderingPolicy.ONLY_NON_SYNTHESIZED
        }

        public val ONLY_NAMES_WITH_SHORT_TYPES: DescriptorRenderer = withOptions {
            withDefinedIn = false
            modifiers = emptySet()
            nameShortness = NameShortness.SHORT
            withoutTypeParameters = true
            parameterNameRenderingPolicy = ParameterNameRenderingPolicy.NONE
            receiverAfterName = true
            renderCompanionObjectName = true
            withoutSuperTypes = true
            startFromName = true
        }

        public val FQ_NAMES_IN_TYPES: DescriptorRenderer = withOptions {
            modifiers = DescriptorRendererModifier.ALL
        }

        public val SHORT_NAMES_IN_TYPES: DescriptorRenderer = withOptions {
            nameShortness = NameShortness.SHORT
            parameterNameRenderingPolicy = ParameterNameRenderingPolicy.ONLY_NON_SYNTHESIZED
        }

        public val DEBUG_TEXT: DescriptorRenderer = withOptions {
            debugMode = true
            nameShortness = NameShortness.FULLY_QUALIFIED
            modifiers = DescriptorRendererModifier.ALL
        }

        public val FLEXIBLE_TYPES_FOR_CODE: DescriptorRenderer = withOptions {
            flexibleTypesForCode = true
        }

        public val HTML: DescriptorRenderer = withOptions {
            textFormat = RenderingFormat.HTML
            modifiers = DescriptorRendererModifier.ALL
        }

        public fun getClassKindPrefix(klass: ClassDescriptor): String {
            if (klass.isCompanionObject()) {
                return "companion object"
            }
            return when (klass.getKind()) {
                ClassKind.CLASS -> "class"
                ClassKind.INTERFACE -> "interface"
                ClassKind.ENUM_CLASS -> "enum class"
                ClassKind.OBJECT -> "object"
                ClassKind.ANNOTATION_CLASS -> "class"
                ClassKind.ENUM_ENTRY -> "enum entry"
            }
        }
    }
}

public interface DescriptorRendererOptions {
    public var nameShortness: NameShortness
    public var withDefinedIn: Boolean
    public var modifiers: Set<DescriptorRendererModifier>
    public var startFromName: Boolean
    public var debugMode: Boolean
    public var classWithPrimaryConstructor: Boolean
    public var verbose: Boolean
    public var unitReturnType: Boolean
    public var withoutReturnType: Boolean
    public var normalizedVisibilities: Boolean
    public var showInternalKeyword: Boolean
    public var prettyFunctionTypes: Boolean
    public var uninferredTypeParameterAsName: Boolean
    public var overrideRenderingPolicy: OverrideRenderingPolicy
    public var valueParametersHandler: DescriptorRenderer.ValueParametersHandler
    public var textFormat: RenderingFormat
    public var excludedAnnotationClasses: Set<FqName>
    public var excludedTypeAnnotationClasses: Set<FqName>
    public var includePropertyConstant: Boolean
    public var parameterNameRenderingPolicy: ParameterNameRenderingPolicy
    public var withoutTypeParameters: Boolean
    public var receiverAfterName: Boolean
    public var renderCompanionObjectName: Boolean
    public var withoutSuperTypes: Boolean
    public var typeNormalizer: (JetType) -> JetType
    public var renderDefaultValues: Boolean
    public var flexibleTypesForCode: Boolean
    public var secondaryConstructorsAsPrimary: Boolean
    public var renderAccessors: Boolean
    public var renderDefaultAnnotationArguments: Boolean
}

public enum class RenderingFormat {
    PLAIN,
    HTML
}

public enum class NameShortness {
    SHORT,
    FULLY_QUALIFIED,
    SOURCE_CODE_QUALIFIED // for local declarations qualified up to function scope
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

public enum class DescriptorRendererModifier(val includeByDefault: Boolean) {
    VISIBILITY(true),
    MODALITY(true),
    OVERRIDE(true),
    ANNOTATIONS(false),
    INNER(true),
    MEMBER_KIND(true)

    ;

    companion object {
        val DEFAULTS = DescriptorRendererModifier.values().filter { it.includeByDefault }.toSet()
        val ALL = DescriptorRendererModifier.values().toSet()
    }
}
