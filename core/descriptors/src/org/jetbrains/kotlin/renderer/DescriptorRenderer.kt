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

package org.jetbrains.kotlin.renderer

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.FqNameUnsafe
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeConstructor
import org.jetbrains.kotlin.types.TypeProjection

abstract class DescriptorRenderer {
    fun withOptions(changeOptions: DescriptorRendererOptions.() -> Unit): DescriptorRenderer {
        val options = (this as DescriptorRendererImpl).options.copy()
        options.changeOptions()
        options.lock()
        return DescriptorRendererImpl(options)
    }

    abstract fun renderMessage(message: String): String

    abstract fun renderType(type: KotlinType): String

    abstract fun renderFlexibleType(lowerRendered: String, upperRendered: String, builtIns: KotlinBuiltIns): String

    abstract fun renderTypeArguments(typeArguments: List<TypeProjection>): String

    abstract fun renderTypeProjection(typeProjection: TypeProjection): String

    abstract fun renderTypeConstructor(typeConstructor: TypeConstructor): String

    abstract fun renderClassifierName(klass: ClassifierDescriptor): String

    abstract fun renderAnnotation(annotation: AnnotationDescriptor, target: AnnotationUseSiteTarget? = null): String

    abstract fun render(declarationDescriptor: DeclarationDescriptor): String

    abstract fun renderValueParameters(parameters: Collection<ValueParameterDescriptor>, synthesizedParameterNames: Boolean): String

    fun renderFunctionParameters(functionDescriptor: FunctionDescriptor): String
            = renderValueParameters(functionDescriptor.valueParameters, functionDescriptor.hasSynthesizedParameterNames())

    abstract fun renderName(name: Name): String

    abstract fun renderFqName(fqName: FqNameUnsafe): String

    interface ValueParametersHandler {
        fun appendBeforeValueParameters(parameterCount: Int, builder: StringBuilder)
        fun appendAfterValueParameters(parameterCount: Int, builder: StringBuilder)

        fun appendBeforeValueParameter(parameter: ValueParameterDescriptor, parameterIndex: Int, parameterCount: Int, builder: StringBuilder)
        fun appendAfterValueParameter(parameter: ValueParameterDescriptor, parameterIndex: Int, parameterCount: Int, builder: StringBuilder)

        object DEFAULT : ValueParametersHandler {
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
        fun withOptions(changeOptions: DescriptorRendererOptions.() -> Unit): DescriptorRenderer {
            val options = DescriptorRendererOptionsImpl()
            options.changeOptions()
            options.lock()
            return DescriptorRendererImpl(options)
        }

        @JvmField val COMPACT_WITH_MODIFIERS: DescriptorRenderer = withOptions {
            withDefinedIn = false
        }

        @JvmField val COMPACT: DescriptorRenderer = withOptions {
            withDefinedIn = false
            modifiers = emptySet()
        }

        @JvmField val COMPACT_WITH_SHORT_TYPES: DescriptorRenderer = withOptions {
            modifiers = emptySet()
            classifierNamePolicy = ClassifierNamePolicy.SHORT
            parameterNameRenderingPolicy = ParameterNameRenderingPolicy.ONLY_NON_SYNTHESIZED
        }

        @JvmField val ONLY_NAMES_WITH_SHORT_TYPES: DescriptorRenderer = withOptions {
            withDefinedIn = false
            modifiers = emptySet()
            classifierNamePolicy = ClassifierNamePolicy.SHORT
            withoutTypeParameters = true
            parameterNameRenderingPolicy = ParameterNameRenderingPolicy.NONE
            receiverAfterName = true
            renderCompanionObjectName = true
            withoutSuperTypes = true
            startFromName = true
        }

        @JvmField val FQ_NAMES_IN_TYPES: DescriptorRenderer = withOptions {
            modifiers = DescriptorRendererModifier.ALL
        }

        @JvmField val SHORT_NAMES_IN_TYPES: DescriptorRenderer = withOptions {
            classifierNamePolicy = ClassifierNamePolicy.SHORT
            parameterNameRenderingPolicy = ParameterNameRenderingPolicy.ONLY_NON_SYNTHESIZED
        }

        @JvmField val DEBUG_TEXT: DescriptorRenderer = withOptions {
            debugMode = true
            classifierNamePolicy = ClassifierNamePolicy.FULLY_QUALIFIED
            modifiers = DescriptorRendererModifier.ALL
        }

        @JvmField val HTML: DescriptorRenderer = withOptions {
            textFormat = RenderingFormat.HTML
            modifiers = DescriptorRendererModifier.ALL
        }

        fun getClassKindPrefix(klass: ClassDescriptor): String {
            if (klass.isCompanionObject) {
                return "companion object"
            }
            return when (klass.kind) {
                ClassKind.CLASS -> "class"
                ClassKind.INTERFACE -> "interface"
                ClassKind.ENUM_CLASS -> "enum class"
                ClassKind.OBJECT -> "object"
                ClassKind.ANNOTATION_CLASS -> "annotation class"
                ClassKind.ENUM_ENTRY -> "enum entry"
            }
        }
    }
}

interface DescriptorRendererOptions {
    var classifierNamePolicy: ClassifierNamePolicy
    var withDefinedIn: Boolean
    var modifiers: Set<DescriptorRendererModifier>
    var startFromName: Boolean
    var debugMode: Boolean
    var classWithPrimaryConstructor: Boolean
    var verbose: Boolean
    var unitReturnType: Boolean
    var withoutReturnType: Boolean
    var normalizedVisibilities: Boolean
    var showInternalKeyword: Boolean
    var uninferredTypeParameterAsName: Boolean
    var overrideRenderingPolicy: OverrideRenderingPolicy
    var valueParametersHandler: DescriptorRenderer.ValueParametersHandler
    var textFormat: RenderingFormat
    var excludedAnnotationClasses: Set<FqName>
    var excludedTypeAnnotationClasses: Set<FqName>
    var includeAnnotationArguments: Boolean
    var includePropertyConstant: Boolean
    var parameterNameRenderingPolicy: ParameterNameRenderingPolicy
    var withoutTypeParameters: Boolean
    var receiverAfterName: Boolean
    var renderCompanionObjectName: Boolean
    var withoutSuperTypes: Boolean
    var typeNormalizer: (KotlinType) -> KotlinType
    var renderDefaultValues: Boolean
    var secondaryConstructorsAsPrimary: Boolean
    var renderAccessors: Boolean
    var renderDefaultAnnotationArguments: Boolean
    var alwaysRenderModifiers: Boolean
    var renderConstructorKeyword: Boolean
    var renderUnabbreviatedType: Boolean
    var includeAdditionalModifiers: Boolean
    var parameterNamesInFunctionalTypes: Boolean
}

object ExcludedTypeAnnotations {
    val annotationsForNullabilityAndMutability = setOf(
            FqName("org.jetbrains.annotations.ReadOnly"),
            FqName("org.jetbrains.annotations.Mutable"),
            FqName("org.jetbrains.annotations.NotNull"),
            FqName("org.jetbrains.annotations.Nullable"),
            FqName("android.support.annotation.Nullable"),
            FqName("android.support.annotation.NonNull"),
            FqName("com.android.annotations.Nullable"),
            FqName("com.android.annotations.NonNull"),
            FqName("org.eclipse.jdt.annotation.Nullable"),
            FqName("org.eclipse.jdt.annotation.NonNull"),
            FqName("org.checkerframework.checker.nullness.qual.Nullable"),
            FqName("org.checkerframework.checker.nullness.qual.NonNull"),
            FqName("javax.annotation.Nonnull"),
            FqName("javax.annotation.Nullable"),
            FqName("javax.annotation.CheckForNull"),
            FqName("edu.umd.cs.findbugs.annotations.NonNull"),
            FqName("edu.umd.cs.findbugs.annotations.CheckForNull"),
            FqName("edu.umd.cs.findbugs.annotations.Nullable"),
            FqName("edu.umd.cs.findbugs.annotations.PossiblyNull"),
            FqName("lombok.NonNull")
    )

    val internalAnnotationsForResolve = setOf(
            FqName("kotlin.internal.NoInfer"),
            FqName("kotlin.internal.Exact")
    )
}

enum class RenderingFormat {
    PLAIN {
        override fun escape(string: String) = string;
    },
    HTML {
        override fun escape(string: String) = string.replace("<", "&lt;").replace(">", "&gt;")
    };

    abstract fun escape(string: String): String
}

enum class OverrideRenderingPolicy {
    RENDER_OVERRIDE,
    RENDER_OPEN,
    RENDER_OPEN_OVERRIDE
}

enum class ParameterNameRenderingPolicy {
    ALL,
    ONLY_NON_SYNTHESIZED,
    NONE
}

enum class DescriptorRendererModifier(val includeByDefault: Boolean) {
    VISIBILITY(true),
    MODALITY(true),
    OVERRIDE(true),
    ANNOTATIONS(false),
    INNER(true),
    MEMBER_KIND(true),
    DATA(true)

    ;

    companion object {
        @JvmField
        val DEFAULTS = DescriptorRendererModifier.values().filter { it.includeByDefault }.toSet()

        @JvmField
        val ALL = DescriptorRendererModifier.values().toSet()
    }
}
