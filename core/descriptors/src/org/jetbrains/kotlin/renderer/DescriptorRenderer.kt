/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.renderer

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.FqNameUnsafe
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.constants.ConstantValue
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

    fun renderFunctionParameters(functionDescriptor: FunctionDescriptor): String =
        renderValueParameters(functionDescriptor.valueParameters, functionDescriptor.hasSynthesizedParameterNames())

    abstract fun renderName(name: Name, rootRenderedElement: Boolean): String

    abstract fun renderFqName(fqName: FqNameUnsafe): String

    interface ValueParametersHandler {
        fun appendBeforeValueParameters(parameterCount: Int, builder: StringBuilder)
        fun appendAfterValueParameters(parameterCount: Int, builder: StringBuilder)

        fun appendBeforeValueParameter(
            parameter: ValueParameterDescriptor,
            parameterIndex: Int,
            parameterCount: Int,
            builder: StringBuilder
        )

        fun appendAfterValueParameter(parameter: ValueParameterDescriptor, parameterIndex: Int, parameterCount: Int, builder: StringBuilder)

        object DEFAULT : ValueParametersHandler {
            override fun appendBeforeValueParameters(parameterCount: Int, builder: StringBuilder) {
                builder.append("(")
            }

            override fun appendAfterValueParameters(parameterCount: Int, builder: StringBuilder) {
                builder.append(")")
            }

            override fun appendBeforeValueParameter(
                parameter: ValueParameterDescriptor,
                parameterIndex: Int,
                parameterCount: Int,
                builder: StringBuilder
            ) {
            }

            override fun appendAfterValueParameter(
                parameter: ValueParameterDescriptor,
                parameterIndex: Int,
                parameterCount: Int,
                builder: StringBuilder
            ) {
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

        @JvmField
        val WITHOUT_MODIFIERS: DescriptorRenderer = withOptions {
            modifiers = emptySet()
        }

        @JvmField
        val COMPACT_WITH_MODIFIERS: DescriptorRenderer = withOptions {
            withDefinedIn = false
        }

        @JvmField
        val COMPACT: DescriptorRenderer = withOptions {
            withDefinedIn = false
            modifiers = emptySet()
        }

        @JvmField
        val COMPACT_WITHOUT_SUPERTYPES: DescriptorRenderer = withOptions {
            withDefinedIn = false
            modifiers = emptySet()
            withoutSuperTypes = true
        }

        @JvmField
        val COMPACT_WITH_SHORT_TYPES: DescriptorRenderer = withOptions {
            modifiers = emptySet()
            classifierNamePolicy = ClassifierNamePolicy.SHORT
            parameterNameRenderingPolicy = ParameterNameRenderingPolicy.ONLY_NON_SYNTHESIZED
        }

        @JvmField
        val ONLY_NAMES_WITH_SHORT_TYPES: DescriptorRenderer = withOptions {
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

        @JvmField
        val FQ_NAMES_IN_TYPES: DescriptorRenderer = withOptions {
            modifiers = DescriptorRendererModifier.ALL_EXCEPT_ANNOTATIONS
        }

        @JvmField
        val FQ_NAMES_IN_TYPES_WITH_ANNOTATIONS: DescriptorRenderer = withOptions {
            modifiers = DescriptorRendererModifier.ALL
        }

        @JvmField
        val SHORT_NAMES_IN_TYPES: DescriptorRenderer = withOptions {
            classifierNamePolicy = ClassifierNamePolicy.SHORT
            parameterNameRenderingPolicy = ParameterNameRenderingPolicy.ONLY_NON_SYNTHESIZED
        }

        @JvmField
        val DEBUG_TEXT: DescriptorRenderer = withOptions {
            debugMode = true
            classifierNamePolicy = ClassifierNamePolicy.FULLY_QUALIFIED
            modifiers = DescriptorRendererModifier.ALL
        }

        @JvmField
        val HTML: DescriptorRenderer = withOptions {
            textFormat = RenderingFormat.HTML
            modifiers = DescriptorRendererModifier.ALL
        }

        fun getClassifierKindPrefix(classifier: ClassifierDescriptorWithTypeParameters): String = when (classifier) {
            is TypeAliasDescriptor ->
                "typealias"
            is ClassDescriptor ->
                if (classifier.isCompanionObject) {
                    "companion object"
                } else when (classifier.kind) {
                    ClassKind.CLASS -> "class"
                    ClassKind.INTERFACE -> "interface"
                    ClassKind.ENUM_CLASS -> "enum class"
                    ClassKind.OBJECT -> "object"
                    ClassKind.ANNOTATION_CLASS -> "annotation class"
                    ClassKind.ENUM_ENTRY -> "enum entry"
                }
            else ->
                throw AssertionError("Unexpected classifier: $classifier")
        }
    }
}

enum class AnnotationArgumentsRenderingPolicy(
    val includeAnnotationArguments: Boolean = false,
    val includeEmptyAnnotationArguments: Boolean = false
) {
    NO_ARGUMENTS,
    UNLESS_EMPTY(true),
    ALWAYS_PARENTHESIZED(includeAnnotationArguments = true, includeEmptyAnnotationArguments = true)
}

interface DescriptorRendererOptions {
    var classifierNamePolicy: ClassifierNamePolicy
    var withDefinedIn: Boolean
    var withSourceFileForTopLevel: Boolean
    var modifiers: Set<DescriptorRendererModifier>
    var startFromName: Boolean
    var startFromDeclarationKeyword: Boolean
    var debugMode: Boolean
    var classWithPrimaryConstructor: Boolean
    var verbose: Boolean
    var unitReturnType: Boolean
    var enhancedTypes: Boolean
    var withoutReturnType: Boolean
    var normalizedVisibilities: Boolean
    var renderDefaultVisibility: Boolean
    var renderDefaultModality: Boolean
    var renderConstructorDelegation: Boolean
    var renderPrimaryConstructorParametersAsProperties: Boolean
    var actualPropertiesInPrimaryConstructor: Boolean
    var uninferredTypeParameterAsName: Boolean
    var overrideRenderingPolicy: OverrideRenderingPolicy
    var valueParametersHandler: DescriptorRenderer.ValueParametersHandler
    var textFormat: RenderingFormat
    var excludedAnnotationClasses: Set<FqName>
    var excludedTypeAnnotationClasses: Set<FqName>
    var annotationFilter: ((AnnotationDescriptor) -> Boolean)?
    var eachAnnotationOnNewLine: Boolean

    var annotationArgumentsRenderingPolicy: AnnotationArgumentsRenderingPolicy
    val includeAnnotationArguments: Boolean get() = annotationArgumentsRenderingPolicy.includeAnnotationArguments
    val includeEmptyAnnotationArguments: Boolean get() = annotationArgumentsRenderingPolicy.includeEmptyAnnotationArguments

    var boldOnlyForNamesInHtml: Boolean

    var includePropertyConstant: Boolean
    var propertyConstantRenderer: ((ConstantValue<*>) -> String?)?
    var parameterNameRenderingPolicy: ParameterNameRenderingPolicy
    var withoutTypeParameters: Boolean
    var receiverAfterName: Boolean
    var renderCompanionObjectName: Boolean
    var withoutSuperTypes: Boolean
    var typeNormalizer: (KotlinType) -> KotlinType
    var defaultParameterValueRenderer: ((ValueParameterDescriptor) -> String)?
    var secondaryConstructorsAsPrimary: Boolean
    var propertyAccessorRenderingPolicy: PropertyAccessorRenderingPolicy
    var renderDefaultAnnotationArguments: Boolean
    var alwaysRenderModifiers: Boolean
    var renderConstructorKeyword: Boolean
    var renderUnabbreviatedType: Boolean
    var renderTypeExpansions: Boolean
    var includeAdditionalModifiers: Boolean
    var parameterNamesInFunctionalTypes: Boolean
    var renderFunctionContracts: Boolean
    var presentableUnresolvedTypes: Boolean
    var informativeErrorType: Boolean
}

object ExcludedTypeAnnotations {
    val internalAnnotationsForResolve = setOf(
        FqName("kotlin.internal.NoInfer"),
        FqName("kotlin.internal.Exact")
    )
}

enum class RenderingFormat {
    PLAIN {
        override fun escape(string: String) = string
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

enum class PropertyAccessorRenderingPolicy {
    PRETTY,
    DEBUG,
    NONE
}

enum class DescriptorRendererModifier(val includeByDefault: Boolean) {
    VISIBILITY(true),
    MODALITY(true),
    OVERRIDE(true),
    ANNOTATIONS(false),
    INNER(true),
    MEMBER_KIND(true),
    DATA(true),
    INLINE(true),
    EXPECT(true),
    ACTUAL(true),
    CONST(true),
    LATEINIT(true),
    FUN(true),
    VALUE(true)
    ;

    companion object {
        @JvmField
        val ALL_EXCEPT_ANNOTATIONS = values().filter { it.includeByDefault }.toSet()

        @JvmField
        val ALL = values().toSet()
    }
}
