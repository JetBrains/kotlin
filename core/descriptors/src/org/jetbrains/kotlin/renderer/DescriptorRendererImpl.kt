/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.renderer

import org.jetbrains.kotlin.builtins.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotated
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.FqNameUnsafe
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils.isCompanionObject
import org.jetbrains.kotlin.resolve.constants.AnnotationValue
import org.jetbrains.kotlin.resolve.constants.ArrayValue
import org.jetbrains.kotlin.resolve.constants.ConstantValue
import org.jetbrains.kotlin.resolve.constants.KClassValue
import org.jetbrains.kotlin.resolve.descriptorUtil.annotationClass
import org.jetbrains.kotlin.resolve.descriptorUtil.declaresOrInheritsDefaultValue
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.ErrorUtils.UninferredParameterTypeConstructor
import org.jetbrains.kotlin.types.TypeUtils.CANT_INFER_FUNCTION_PARAM_TYPE
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly

internal class DescriptorRendererImpl(
    val options: DescriptorRendererOptionsImpl
) : DescriptorRenderer(), DescriptorRendererOptions by options/* this gives access to options without qualifier */ {
    init {
        assert(options.isLocked)
    }

    private val functionTypeAnnotationsRenderer: DescriptorRendererImpl by lazy {
        withOptions {
            excludedTypeAnnotationClasses += listOf(StandardNames.FqNames.extensionFunctionType, StandardNames.FqNames.contextFunctionTypeParams)
        } as DescriptorRendererImpl
    }

    /* FORMATTING */
    private fun renderKeyword(keyword: String): String = when (textFormat) {
        RenderingFormat.PLAIN -> keyword
        RenderingFormat.HTML -> if (boldOnlyForNamesInHtml) keyword else "<b>$keyword</b>"
    }

    private fun renderError(keyword: String): String = when (textFormat) {
        RenderingFormat.PLAIN -> keyword
        RenderingFormat.HTML -> "<font color=red><b>$keyword</b></font>"
    }

    private fun escape(string: String) = textFormat.escape(string)

    private fun lt() = escape("<")
    private fun gt() = escape(">")

    private fun arrow(): String = when (textFormat) {
        RenderingFormat.PLAIN -> escape("->")
        RenderingFormat.HTML -> "&rarr;"
    }

    override fun renderMessage(message: String): String = when (textFormat) {
        RenderingFormat.PLAIN -> message
        RenderingFormat.HTML -> "<i>$message</i>"
    }

    /* NAMES RENDERING */
    override fun renderName(name: Name, rootRenderedElement: Boolean): String {
        val escaped = escape(name.render())
        return if (boldOnlyForNamesInHtml && textFormat == RenderingFormat.HTML && rootRenderedElement) {
            "<b>$escaped</b>"
        } else
            escaped
    }

    private fun renderName(descriptor: DeclarationDescriptor, builder: StringBuilder, rootRenderedElement: Boolean) {
        builder.append(renderName(descriptor.name, rootRenderedElement))
    }

    private fun renderCompanionObjectName(descriptor: DeclarationDescriptor, builder: StringBuilder) {
        if (renderCompanionObjectName) {
            if (startFromName) {
                builder.append("companion object")
            }
            renderSpaceIfNeeded(builder)
            val containingDeclaration = descriptor.containingDeclaration
            if (containingDeclaration != null) {
                builder.append("of ")
                builder.append(renderName(containingDeclaration.name, false))
            }
        }
        if (verbose || descriptor.name != SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT) {
            if (!startFromName) renderSpaceIfNeeded(builder)
            builder.append(renderName(descriptor.name, true))
        }
    }

    override fun renderFqName(fqName: FqNameUnsafe) = renderFqName(fqName.pathSegments())

    private fun renderFqName(pathSegments: List<Name>) = escape(org.jetbrains.kotlin.renderer.renderFqName(pathSegments))

    override fun renderClassifierName(klass: ClassifierDescriptor): String = if (ErrorUtils.isError(klass)) {
        klass.typeConstructor.toString()
    } else
        classifierNamePolicy.renderClassifier(klass, this)

    /* TYPES RENDERING */
    override fun renderType(type: KotlinType): String = buildString {
        renderNormalizedType(typeNormalizer(type))
    }

    private fun StringBuilder.renderNormalizedType(type: KotlinType) {
        val abbreviated = type.unwrap() as? AbbreviatedType
        if (abbreviated != null) {
            if (renderTypeExpansions) {
                renderNormalizedTypeAsIs(abbreviated.expandedType)
            } else {
                // TODO nullability is lost for abbreviated type?
                renderNormalizedTypeAsIs(abbreviated.abbreviation)
                if (renderUnabbreviatedType) {
                    renderAbbreviatedTypeExpansion(abbreviated)
                }
            }
            return
        }

        renderNormalizedTypeAsIs(type)
    }

    private fun StringBuilder.renderAbbreviatedTypeExpansion(abbreviated: AbbreviatedType) {
        if (textFormat == RenderingFormat.HTML) {
            append("<font color=\"808080\"><i>")
        }
        append(" /* = ")
        renderNormalizedTypeAsIs(abbreviated.expandedType)
        append(" */")
        if (textFormat == RenderingFormat.HTML) {
            append("</i></font>")
        }
    }

    private fun StringBuilder.renderNormalizedTypeAsIs(type: KotlinType) {
        if (type is WrappedType && debugMode && !type.isComputed()) {
            append("<Not computed yet>")
            return
        }
        when (val unwrappedType = type.unwrap()) {
            is FlexibleType -> append(unwrappedType.render(this@DescriptorRendererImpl, this@DescriptorRendererImpl))
            is SimpleType -> renderSimpleType(unwrappedType)
        }
    }

    private fun StringBuilder.renderSimpleType(type: SimpleType) {
        if (type == CANT_INFER_FUNCTION_PARAM_TYPE || TypeUtils.isDontCarePlaceholder(type)) {
            append("???")
            return
        }
        if (ErrorUtils.isUninferredParameter(type)) {
            if (uninferredTypeParameterAsName) {
                append(renderError((type.constructor as UninferredParameterTypeConstructor).typeParameterDescriptor.name.toString()))
            } else {
                append("???")
            }
            return
        }

        if (type.isError) {
            renderDefaultType(type)
            return
        }
        if (shouldRenderAsPrettyFunctionType(type)) {
            renderFunctionType(type)
        } else {
            renderDefaultType(type)
        }
    }

    private fun shouldRenderAsPrettyFunctionType(type: KotlinType): Boolean {
        return type.isBuiltinFunctionalType && type.arguments.none { it.isStarProjection }
    }

    override fun renderFlexibleType(lowerRendered: String, upperRendered: String, builtIns: KotlinBuiltIns): String {
        if (differsOnlyInNullability(lowerRendered, upperRendered)) {
            if (upperRendered.startsWith("(")) {
                // the case of complex type, e.g. (() -> Unit)?
                return "($lowerRendered)!"
            }
            return "$lowerRendered!"
        }

        val kotlinCollectionsPrefix = classifierNamePolicy.renderClassifier(builtIns.collection, this).substringBefore("Collection")
        val mutablePrefix = "Mutable"
        // java.util.List<Foo> -> (Mutable)List<Foo!>!
        val simpleCollection = replacePrefixes(
            lowerRendered,
            kotlinCollectionsPrefix + mutablePrefix,
            upperRendered,
            kotlinCollectionsPrefix,
            "$kotlinCollectionsPrefix($mutablePrefix)"
        )
        if (simpleCollection != null) return simpleCollection
        // java.util.Map.Entry<Foo, Bar> -> (Mutable)Map.(Mutable)Entry<Foo!, Bar!>!
        val mutableEntry = replacePrefixes(
            lowerRendered,
            kotlinCollectionsPrefix + "MutableMap.MutableEntry",
            upperRendered,
            kotlinCollectionsPrefix + "Map.Entry",
            "$kotlinCollectionsPrefix(Mutable)Map.(Mutable)Entry"
        )
        if (mutableEntry != null) return mutableEntry

        val kotlinPrefix = classifierNamePolicy.renderClassifier(builtIns.array, this).substringBefore("Array")
        // Foo[] -> Array<(out) Foo!>!
        val array = replacePrefixes(
            lowerRendered,
            kotlinPrefix + escape("Array<"),
            upperRendered,
            kotlinPrefix + escape("Array<out "),
            kotlinPrefix + escape("Array<(out) ")
        )
        if (array != null) return array

        return "($lowerRendered..$upperRendered)"
    }

    override fun renderTypeArguments(typeArguments: List<TypeProjection>): String = if (typeArguments.isEmpty()) ""
    else buildString {
        append(lt())
        this.appendTypeProjections(typeArguments)
        append(gt())
    }

    private fun StringBuilder.renderDefaultType(type: KotlinType) {
        this.renderAnnotations(type)

        val originalTypeOfDefNotNullType = (type as? DefinitelyNotNullType)?.original

        when {
            type.isError -> {
                if (type is UnresolvedType && presentableUnresolvedTypes) {
                    append(type.presentableName)
                } else {
                    if (type is ErrorType && !informativeErrorType) {
                        append(type.presentableName)
                    } else {
                        append(type.constructor.toString()) // Debug name of an error type is more informative
                    }
                }
                append(renderTypeArguments(type.arguments))
            }
            type is StubTypeForBuilderInference ->
                append(type.originalTypeVariable.toString())
            originalTypeOfDefNotNullType is StubTypeForBuilderInference ->
                append(originalTypeOfDefNotNullType.originalTypeVariable.toString())
            else -> renderTypeConstructorAndArguments(type)
        }

        if (type.isMarkedNullable) {
            append("?")
        }

        if (type.isDefinitelyNotNullType) {
            append(" & Any")
        }
    }

    private fun StringBuilder.renderTypeConstructorAndArguments(
        type: KotlinType,
        typeConstructor: TypeConstructor = type.constructor
    ) {
        val possiblyInnerType = type.buildPossiblyInnerType()
        if (possiblyInnerType == null) {
            append(renderTypeConstructor(typeConstructor))
            append(renderTypeArguments(type.arguments))
            return
        }

        renderPossiblyInnerType(possiblyInnerType)
    }

    private fun StringBuilder.renderPossiblyInnerType(possiblyInnerType: PossiblyInnerType) {
        possiblyInnerType.outerType?.let {
            renderPossiblyInnerType(it)
            append('.')
            append(renderName(possiblyInnerType.classifierDescriptor.name, false))
        } ?: append(renderTypeConstructor(possiblyInnerType.classifierDescriptor.typeConstructor))

        append(renderTypeArguments(possiblyInnerType.arguments))
    }

    override fun renderTypeConstructor(typeConstructor: TypeConstructor): String = when (val cd = typeConstructor.declarationDescriptor) {
        is TypeParameterDescriptor, is ClassDescriptor, is TypeAliasDescriptor -> renderClassifierName(cd)
        null -> {
            if (typeConstructor is IntersectionTypeConstructor) {
                typeConstructor.makeDebugNameForIntersectionType { if (it is StubTypeForBuilderInference) it.originalTypeVariable else it }
            } else typeConstructor.toString()
        }
        else -> error("Unexpected classifier: " + cd::class.java)
    }

    override fun renderTypeProjection(typeProjection: TypeProjection) = buildString {
        appendTypeProjections(listOf(typeProjection))
    }

    private fun StringBuilder.appendTypeProjections(typeProjections: List<TypeProjection>) {
        typeProjections.joinTo(this, ", ") {
            if (it.isStarProjection) {
                "*"
            } else {
                val type = renderType(it.type)
                if (it.projectionKind == Variance.INVARIANT) type else "${it.projectionKind} $type"
            }
        }
    }

    private fun StringBuilder.renderFunctionType(type: KotlinType) {
        val lengthBefore = length
        // we need special renderer to skip @ExtensionFunctionType and @ContextFunctionTypeParams
        with(functionTypeAnnotationsRenderer) {
            renderAnnotations(type)
        }
        val hasAnnotations = length != lengthBefore

        val receiverType = type.getReceiverTypeFromFunctionType()
        val contextReceiversTypes = type.getContextReceiverTypesFromFunctionType()
        if (contextReceiversTypes.isNotEmpty()) {
            append("context(")
            val withoutLast = contextReceiversTypes.subList(0, contextReceiversTypes.lastIndex)
            for (contextReceiverType in withoutLast) {
                renderNormalizedType(contextReceiverType)
                append(", ")
            }
            renderNormalizedType(contextReceiversTypes.last())
            append(") ")
        }

        val isSuspend = type.isSuspendFunctionType
        val isNullable = type.isMarkedNullable

        val needParenthesis = isNullable || (hasAnnotations && receiverType != null)
        if (needParenthesis) {
            if (isSuspend) {
                insert(lengthBefore, '(')
            } else {
                if (hasAnnotations) {
                    assert(last().isWhitespace())
                    if (get(lastIndex - 1) != ')') {
                        // last annotation rendered without parenthesis - need to add them otherwise parsing will be incorrect
                        insert(lastIndex, "()")
                    }
                }

                append("(")
            }
        }

        renderModifier(this, isSuspend, "suspend")

        if (receiverType != null) {
            val surroundReceiver = shouldRenderAsPrettyFunctionType(receiverType) && !receiverType.isMarkedNullable ||
                    receiverType.hasModifiersOrAnnotations()
            if (surroundReceiver) {
                append("(")
            }
            renderNormalizedType(receiverType)
            if (surroundReceiver) {
                append(")")
            }
            append(".")
        }

        append("(")

        val parameterTypes = type.getValueParameterTypesFromFunctionType()
        for ((index, typeProjection) in parameterTypes.withIndex()) {
            if (index > 0) append(", ")

            val name = if (parameterNamesInFunctionalTypes) typeProjection.type.extractParameterNameFromFunctionTypeArgument() else null
            if (name != null) {
                append(renderName(name, false))
                append(": ")
            }

            append(renderTypeProjection(typeProjection))
        }

        append(") ").append(arrow()).append(" ")
        renderNormalizedType(type.getReturnTypeFromFunctionType())

        if (needParenthesis) append(")")

        if (isNullable) append("?")
    }

    private fun KotlinType.hasModifiersOrAnnotations() =
        isSuspendFunctionType || !annotations.isEmpty()

    /* METHODS FOR ALL KINDS OF DESCRIPTORS */
    private fun StringBuilder.appendDefinedIn(descriptor: DeclarationDescriptor) {
        if (descriptor is PackageFragmentDescriptor || descriptor is PackageViewDescriptor) {
            return
        }
        
        val containingDeclaration = descriptor.containingDeclaration
        if (containingDeclaration != null && containingDeclaration !is ModuleDescriptor) {
            append(" ").append(renderMessage("defined in")).append(" ")
            val fqName = DescriptorUtils.getFqName(containingDeclaration)
            append(if (fqName.isRoot) "root package" else renderFqName(fqName))

            if (withSourceFileForTopLevel &&
                containingDeclaration is PackageFragmentDescriptor &&
                descriptor is DeclarationDescriptorWithSource
            ) {
                descriptor.source.containingFile.name?.let { sourceFileName ->
                    append(" ").append(renderMessage("in file")).append(" ").append(sourceFileName)
                }
            }
        }
    }

    private fun StringBuilder.renderAnnotations(annotated: Annotated, target: AnnotationUseSiteTarget? = null) {
        if (DescriptorRendererModifier.ANNOTATIONS !in modifiers) return

        val excluded = if (annotated is KotlinType) excludedTypeAnnotationClasses else excludedAnnotationClasses

        val annotationFilter = annotationFilter
        for (annotation in annotated.annotations) {
            if (annotation.fqName !in excluded
                && !annotation.isParameterName()
                && (annotationFilter == null || annotationFilter(annotation))
            ) {
                append(renderAnnotation(annotation, target))
                if (eachAnnotationOnNewLine) {
                    appendLine()
                } else {
                    append(" ")
                }
            }
        }
    }

    private fun AnnotationDescriptor.isParameterName(): Boolean {
        return fqName == StandardNames.FqNames.parameterName
    }

    override fun renderAnnotation(annotation: AnnotationDescriptor, target: AnnotationUseSiteTarget?): String {
        return buildString {
            append('@')
            if (target != null) {
                append(target.renderName + ":")
            }
            val annotationType = annotation.type
            append(renderType(annotationType))

            if (includeAnnotationArguments) {
                val arguments = renderAndSortAnnotationArguments(annotation)
                if (includeEmptyAnnotationArguments || arguments.isNotEmpty()) {
                    arguments.joinTo(this, ", ", "(", ")")
                }
            }

            if (verbose && (annotationType.isError || annotationType.constructor.declarationDescriptor is NotFoundClasses.MockClassDescriptor)) {
                append(" /* annotation class not found */")
            }
        }
    }

    private fun renderAndSortAnnotationArguments(descriptor: AnnotationDescriptor): List<String> {
        val allValueArguments = descriptor.allValueArguments
        val classDescriptor = if (renderDefaultAnnotationArguments) descriptor.annotationClass else null
        val parameterDescriptorsWithDefaultValue = classDescriptor?.unsubstitutedPrimaryConstructor?.valueParameters
            ?.filter { it.declaresDefaultValue() }
            ?.map { it.name }
            .orEmpty()
        val defaultList = parameterDescriptorsWithDefaultValue.filter { it !in allValueArguments }.map { "${it.asString()} = ..." }
        val argumentList = allValueArguments.entries
            .map { (name, value) ->
                "${name.asString()} = ${if (name !in parameterDescriptorsWithDefaultValue) renderConstant(value) else "..."}"
            }
        return (defaultList + argumentList).sorted()
    }

    private fun renderConstant(value: ConstantValue<*>): String {
        return when (value) {
            is ArrayValue -> value.value.joinToString(", ", "{", "}") { renderConstant(it) }
            is AnnotationValue -> renderAnnotation(value.value).removePrefix("@")
            is KClassValue -> when (val classValue = value.value) {
                is KClassValue.Value.LocalClass -> "${classValue.type}::class"
                is KClassValue.Value.NormalClass -> {
                    var type = classValue.classId.asSingleFqName().asString()
                    repeat(classValue.arrayDimensions) { type = "kotlin.Array<$type>" }
                    "$type::class"
                }
            }
            else -> value.toString()
        }
    }

    private fun renderVisibility(visibility: DescriptorVisibility, builder: StringBuilder): Boolean {
        @Suppress("NAME_SHADOWING")
        var visibility = visibility
        if (DescriptorRendererModifier.VISIBILITY !in modifiers) return false
        if (normalizedVisibilities) {
            visibility = visibility.normalize()
        }
        if (!renderDefaultVisibility && visibility == DescriptorVisibilities.DEFAULT_VISIBILITY) return false
        builder.append(renderKeyword(visibility.internalDisplayName)).append(" ")
        return true
    }

    private fun renderModality(modality: Modality, builder: StringBuilder, defaultModality: Modality) {
        if (!renderDefaultModality && modality == defaultModality) return
        renderModifier(builder, DescriptorRendererModifier.MODALITY in modifiers, modality.name.toLowerCaseAsciiOnly())
    }

    private fun MemberDescriptor.implicitModalityWithoutExtensions(): Modality {
        if (this is ClassDescriptor) {
            return if (kind == ClassKind.INTERFACE) Modality.ABSTRACT else Modality.FINAL
        }
        val containingClassDescriptor = containingDeclaration as? ClassDescriptor ?: return Modality.FINAL
        if (this !is CallableMemberDescriptor) return Modality.FINAL
        if (this.overriddenDescriptors.isNotEmpty()) {
            if (containingClassDescriptor.modality != Modality.FINAL) return Modality.OPEN
        }
        return if (containingClassDescriptor.kind == ClassKind.INTERFACE && this.visibility != DescriptorVisibilities.PRIVATE) {
            if (this.modality == Modality.ABSTRACT) Modality.ABSTRACT else Modality.OPEN
        } else
            Modality.FINAL
    }

    private fun renderModalityForCallable(callable: CallableMemberDescriptor, builder: StringBuilder) {
        if (!DescriptorUtils.isTopLevelDeclaration(callable) || callable.modality != Modality.FINAL) {
            if (overrideRenderingPolicy == OverrideRenderingPolicy.RENDER_OVERRIDE && callable.modality == Modality.OPEN &&
                overridesSomething(callable)
            ) {
                return
            }
            renderModality(callable.modality, builder, callable.implicitModalityWithoutExtensions())
        }
    }

    private fun renderOverride(callableMember: CallableMemberDescriptor, builder: StringBuilder) {
        if (DescriptorRendererModifier.OVERRIDE !in modifiers) return
        if (overridesSomething(callableMember)) {
            if (overrideRenderingPolicy != OverrideRenderingPolicy.RENDER_OPEN) {
                renderModifier(builder, true, "override")
                if (verbose) {
                    builder.append("/*").append(callableMember.overriddenDescriptors.size).append("*/ ")
                }
            }
        }
    }

    private fun renderMemberKind(callableMember: CallableMemberDescriptor, builder: StringBuilder) {
        if (DescriptorRendererModifier.MEMBER_KIND !in modifiers) return
        if (verbose && callableMember.kind != CallableMemberDescriptor.Kind.DECLARATION) {
            builder.append("/*").append(callableMember.kind.name.toLowerCaseAsciiOnly()).append("*/ ")
        }
    }

    private fun renderModifier(builder: StringBuilder, value: Boolean, modifier: String) {
        if (value) {
            builder.append(renderKeyword(modifier))
            builder.append(" ")
        }
    }

    private fun renderMemberModifiers(descriptor: MemberDescriptor, builder: StringBuilder) {
        renderModifier(builder, descriptor.isExternal, "external")
        renderModifier(builder, DescriptorRendererModifier.EXPECT in modifiers && descriptor.isExpect, "expect")
        renderModifier(builder, DescriptorRendererModifier.ACTUAL in modifiers && descriptor.isActual, "actual")
    }

    private fun renderAdditionalModifiers(functionDescriptor: FunctionDescriptor, builder: StringBuilder) {
        val isOperator =
            functionDescriptor.isOperator && (functionDescriptor.overriddenDescriptors.none { it.isOperator } || alwaysRenderModifiers)
        val isInfix =
            functionDescriptor.isInfix && (functionDescriptor.overriddenDescriptors.none { it.isInfix } || alwaysRenderModifiers)

        renderModifier(builder, functionDescriptor.isTailrec, "tailrec")
        renderSuspendModifier(functionDescriptor, builder)
        renderModifier(builder, functionDescriptor.isInline, "inline")
        renderModifier(builder, isInfix, "infix")
        renderModifier(builder, isOperator, "operator")
    }

    private fun renderSuspendModifier(functionDescriptor: FunctionDescriptor, builder: StringBuilder) {
        renderModifier(builder, functionDescriptor.isSuspend, "suspend")
    }

    override fun render(declarationDescriptor: DeclarationDescriptor): String {
        return buildString {
            declarationDescriptor.accept(RenderDeclarationDescriptorVisitor(), this)

            if (withDefinedIn) {
                appendDefinedIn(declarationDescriptor)
            }
        }
    }


    /* TYPE PARAMETERS */
    private fun renderTypeParameter(typeParameter: TypeParameterDescriptor, builder: StringBuilder, topLevel: Boolean) {
        if (topLevel) {
            builder.append(lt())
        }

        if (verbose) {
            builder.append("/*").append(typeParameter.index).append("*/ ")
        }

        renderModifier(builder, typeParameter.isReified, "reified")
        val variance = typeParameter.variance.label
        renderModifier(builder, variance.isNotEmpty(), variance)

        builder.renderAnnotations(typeParameter)

        renderName(typeParameter, builder, topLevel)
        val upperBoundsCount = typeParameter.upperBounds.size
        if ((upperBoundsCount > 1 && !topLevel) || upperBoundsCount == 1) {
            val upperBound = typeParameter.upperBounds.iterator().next()
            if (!KotlinBuiltIns.isDefaultBound(upperBound)) {
                builder.append(" : ").append(renderType(upperBound))
            }
        } else if (topLevel) {
            var first = true
            for (upperBound in typeParameter.upperBounds) {
                if (KotlinBuiltIns.isDefaultBound(upperBound)) {
                    continue
                }
                if (first) {
                    builder.append(" : ")
                } else {
                    builder.append(" & ")
                }
                builder.append(renderType(upperBound))
                first = false
            }
        } else {
            // rendered with "where"
        }

        if (topLevel) {
            builder.append(gt())
        }
    }

    private fun renderTypeParameters(typeParameters: List<TypeParameterDescriptor>, builder: StringBuilder, withSpace: Boolean) {
        if (withoutTypeParameters) return

        if (typeParameters.isNotEmpty()) {
            builder.append(lt())
            renderTypeParameterList(builder, typeParameters)
            builder.append(gt())
            if (withSpace) {
                builder.append(" ")
            }
        }
    }

    private fun renderTypeParameterList(builder: StringBuilder, typeParameters: List<TypeParameterDescriptor>) {
        val iterator = typeParameters.iterator()
        while (iterator.hasNext()) {
            val typeParameterDescriptor = iterator.next()
            renderTypeParameter(typeParameterDescriptor, builder, false)
            if (iterator.hasNext()) {
                builder.append(", ")
            }
        }
    }

    /* FUNCTIONS */
    private fun renderFunction(function: FunctionDescriptor, builder: StringBuilder) {
        if (!startFromName) {
            if (!startFromDeclarationKeyword) {
                builder.renderAnnotations(function)
                renderVisibility(function.visibility, builder)
                renderModalityForCallable(function, builder)

                if (includeAdditionalModifiers) {
                    renderMemberModifiers(function, builder)
                }

                renderOverride(function, builder)

                if (includeAdditionalModifiers) {
                    renderAdditionalModifiers(function, builder)
                } else {
                    renderSuspendModifier(function, builder)
                }

                renderMemberKind(function, builder)

                if (verbose) {
                    if (function.isHiddenToOvercomeSignatureClash) {
                        builder.append("/*isHiddenToOvercomeSignatureClash*/ ")
                    }

                    if (function.isHiddenForResolutionEverywhereBesideSupercalls) {
                        builder.append("/*isHiddenForResolutionEverywhereBesideSupercalls*/ ")
                    }
                }
            }

            builder.append(renderKeyword("fun")).append(" ")
            renderTypeParameters(function.typeParameters, builder, true)
            renderReceiver(function, builder)
        }

        renderName(function, builder, true)

        renderValueParameters(function.valueParameters, function.hasSynthesizedParameterNames(), builder)

        renderReceiverAfterName(function, builder)

        val returnType = function.returnType
        if (!withoutReturnType && (unitReturnType || (returnType == null || !KotlinBuiltIns.isUnit(returnType)))) {
            builder.append(": ").append(if (returnType == null) "[NULL]" else renderType(returnType))
        }

        renderWhereSuffix(function.typeParameters, builder)
    }

    private fun renderReceiverAfterName(callableDescriptor: CallableDescriptor, builder: StringBuilder) {
        if (!receiverAfterName) return

        val receiver = callableDescriptor.extensionReceiverParameter
        if (receiver != null) {
            builder.append(" on ").append(renderType(receiver.type))
        }
    }

    private fun renderReceiver(callableDescriptor: CallableDescriptor, builder: StringBuilder) {
        val receiver = callableDescriptor.extensionReceiverParameter
        if (receiver != null) {
            builder.renderAnnotations(receiver, AnnotationUseSiteTarget.RECEIVER)

            val type = receiver.type
            var result = renderType(type)
            if (shouldRenderAsPrettyFunctionType(type) && !TypeUtils.isNullableType(type)) {
                result = "($result)"
            }
            builder.append(result).append(".")
        }
    }

    private fun renderConstructor(constructor: ConstructorDescriptor, builder: StringBuilder) {
        builder.renderAnnotations(constructor)
        val visibilityRendered = (options.renderDefaultVisibility || constructor.constructedClass.modality != Modality.SEALED)
                && renderVisibility(constructor.visibility, builder)
        renderMemberKind(constructor, builder)

        val constructorKeywordRendered = renderConstructorKeyword || !constructor.isPrimary || visibilityRendered
        if (constructorKeywordRendered) {
            builder.append(renderKeyword("constructor"))
        }
        val classDescriptor = constructor.containingDeclaration
        if (secondaryConstructorsAsPrimary) {
            if (constructorKeywordRendered) {
                builder.append(" ")
            }
            renderName(classDescriptor, builder, true)
            renderTypeParameters(constructor.typeParameters, builder, false)
        }

        renderValueParameters(constructor.valueParameters, constructor.hasSynthesizedParameterNames(), builder)

        if (renderConstructorDelegation && !constructor.isPrimary && classDescriptor is ClassDescriptor) {
            val primaryConstructor = classDescriptor.unsubstitutedPrimaryConstructor
            if (primaryConstructor != null) {
                val parametersWithoutDefault = primaryConstructor.valueParameters.filter {
                    !it.declaresDefaultValue() && it.varargElementType == null
                }
                if (parametersWithoutDefault.isNotEmpty()) {
                    builder.append(" : ").append(renderKeyword("this"))
                    builder.append(parametersWithoutDefault.joinToString(prefix = "(", postfix = ")", separator = ", ") { "" })
                }
            }
        }

        if (secondaryConstructorsAsPrimary) {
            renderWhereSuffix(constructor.typeParameters, builder)
        }
    }

    private fun renderWhereSuffix(typeParameters: List<TypeParameterDescriptor>, builder: StringBuilder) {
        if (withoutTypeParameters) return

        val upperBoundStrings = ArrayList<String>(0)

        for (typeParameter in typeParameters) {
            typeParameter.upperBounds
                .drop(1) // first parameter is rendered by renderTypeParameter
                .mapTo(upperBoundStrings) { renderName(typeParameter.name, false) + " : " + renderType(it) }
        }

        if (upperBoundStrings.isNotEmpty()) {
            builder.append(" ").append(renderKeyword("where")).append(" ")
            upperBoundStrings.joinTo(builder, ", ")
        }
    }

    override fun renderValueParameters(parameters: Collection<ValueParameterDescriptor>, synthesizedParameterNames: Boolean) = buildString {
        renderValueParameters(parameters, synthesizedParameterNames, this)
    }

    private fun renderValueParameters(
        parameters: Collection<ValueParameterDescriptor>,
        synthesizedParameterNames: Boolean,
        builder: StringBuilder
    ) {
        val includeNames = shouldRenderParameterNames(synthesizedParameterNames)
        val parameterCount = parameters.size
        valueParametersHandler.appendBeforeValueParameters(parameterCount, builder)
        for ((index, parameter) in parameters.withIndex()) {
            valueParametersHandler.appendBeforeValueParameter(parameter, index, parameterCount, builder)
            renderValueParameter(parameter, includeNames, builder, false)
            valueParametersHandler.appendAfterValueParameter(parameter, index, parameterCount, builder)
        }
        valueParametersHandler.appendAfterValueParameters(parameterCount, builder)
    }

    private fun shouldRenderParameterNames(synthesizedParameterNames: Boolean): Boolean = when (parameterNameRenderingPolicy) {
        ParameterNameRenderingPolicy.ALL -> true
        ParameterNameRenderingPolicy.ONLY_NON_SYNTHESIZED -> !synthesizedParameterNames
        ParameterNameRenderingPolicy.NONE -> false
    }

    /* VARIABLES */
    private fun renderValueParameter(
        valueParameter: ValueParameterDescriptor,
        includeName: Boolean,
        builder: StringBuilder,
        topLevel: Boolean
    ) {
        if (topLevel) {
            builder.append(renderKeyword("value-parameter")).append(" ")
        }

        if (verbose) {
            builder.append("/*").append(valueParameter.index).append("*/ ")
        }

        builder.renderAnnotations(valueParameter)
        renderModifier(builder, valueParameter.isCrossinline, "crossinline")
        renderModifier(builder, valueParameter.isNoinline, "noinline")

        val isPrimaryConstructor = renderPrimaryConstructorParametersAsProperties &&
                (valueParameter.containingDeclaration as? ClassConstructorDescriptor)?.isPrimary == true
        if (isPrimaryConstructor) {
            renderModifier(builder, actualPropertiesInPrimaryConstructor, "actual")
        }

        renderVariable(valueParameter, includeName, builder, topLevel, isPrimaryConstructor)

        val withDefaultValue =
            defaultParameterValueRenderer != null &&
                    (if (debugMode) valueParameter.declaresDefaultValue() else valueParameter.declaresOrInheritsDefaultValue())
        if (withDefaultValue) {
            builder.append(" = ${defaultParameterValueRenderer!!(valueParameter)}")
        }
    }

    private fun renderValVarPrefix(variable: VariableDescriptor, builder: StringBuilder, isInPrimaryConstructor: Boolean = false) {
        if (isInPrimaryConstructor || variable !is ValueParameterDescriptor) {
            builder.append(renderKeyword(if (variable.isVar) "var" else "val")).append(" ")
        }
    }

    private fun renderVariable(
        variable: VariableDescriptor,
        includeName: Boolean,
        builder: StringBuilder,
        topLevel: Boolean,
        isInPrimaryConstructor: Boolean = false
    ) {
        val realType = variable.type

        val varargElementType = (variable as? ValueParameterDescriptor)?.varargElementType
        val typeToRender = varargElementType ?: realType
        renderModifier(builder, varargElementType != null, "vararg")

        if (isInPrimaryConstructor || topLevel && !startFromName) {
            renderValVarPrefix(variable, builder, isInPrimaryConstructor)
        }

        if (includeName) {
            renderName(variable, builder, topLevel)
            builder.append(": ")
        }

        builder.append(renderType(typeToRender))

        renderInitializer(variable, builder)

        if (verbose && varargElementType != null) {
            builder.append(" /*").append(renderType(realType)).append("*/")
        }
    }

    private fun renderProperty(property: PropertyDescriptor, builder: StringBuilder) {
        if (!startFromName) {
            if (!startFromDeclarationKeyword) {
                renderPropertyAnnotations(property, builder)
                renderVisibility(property.visibility, builder)
                renderModifier(builder, DescriptorRendererModifier.CONST in modifiers && property.isConst, "const")
                renderMemberModifiers(property, builder)
                renderModalityForCallable(property, builder)
                renderOverride(property, builder)
                renderModifier(builder, DescriptorRendererModifier.LATEINIT in modifiers && property.isLateInit, "lateinit")
                renderMemberKind(property, builder)
            }
            renderValVarPrefix(property, builder)
            renderTypeParameters(property.typeParameters, builder, true)
            renderReceiver(property, builder)
        }

        renderName(property, builder, true)
        builder.append(": ").append(renderType(property.type))

        renderReceiverAfterName(property, builder)

        renderInitializer(property, builder)

        renderWhereSuffix(property.typeParameters, builder)
    }

    private fun renderPropertyAnnotations(property: PropertyDescriptor, builder: StringBuilder) {
        if (DescriptorRendererModifier.ANNOTATIONS !in modifiers) return

        builder.renderAnnotations(property)

        property.backingField?.let { builder.renderAnnotations(it, AnnotationUseSiteTarget.FIELD) }
        property.delegateField?.let { builder.renderAnnotations(it, AnnotationUseSiteTarget.PROPERTY_DELEGATE_FIELD) }

        if (propertyAccessorRenderingPolicy == PropertyAccessorRenderingPolicy.NONE) {
            property.getter?.let {
                builder.renderAnnotations(it, AnnotationUseSiteTarget.PROPERTY_GETTER)
            }
            property.setter?.let { setter ->
                setter.let {
                    builder.renderAnnotations(it, AnnotationUseSiteTarget.PROPERTY_SETTER)
                }
                setter.valueParameters.single().let {
                    builder.renderAnnotations(it, AnnotationUseSiteTarget.SETTER_PARAMETER)
                }
            }
        }
    }

    private fun renderInitializer(variable: VariableDescriptor, builder: StringBuilder) {
        if (includePropertyConstant) {
            variable.compileTimeInitializer?.let { constant ->
                builder.append(" = ").append(escape(renderConstant(constant)))
            }
        }
    }

    private fun renderTypeAlias(typeAlias: TypeAliasDescriptor, builder: StringBuilder) {
        builder.renderAnnotations(typeAlias)
        renderVisibility(typeAlias.visibility, builder)
        renderMemberModifiers(typeAlias, builder)
        builder.append(renderKeyword("typealias")).append(" ")
        renderName(typeAlias, builder, true)

        renderTypeParameters(typeAlias.declaredTypeParameters, builder, false)
        renderCapturedTypeParametersIfRequired(typeAlias, builder)

        builder.append(" = ").append(renderType(typeAlias.underlyingType))
    }

    private fun renderCapturedTypeParametersIfRequired(classifier: ClassifierDescriptorWithTypeParameters, builder: StringBuilder) {
        val typeParameters = classifier.declaredTypeParameters
        val typeConstructorParameters = classifier.typeConstructor.parameters

        if (verbose && classifier.isInner && typeConstructorParameters.size > typeParameters.size) {
            builder.append(" /*captured type parameters: ")
            renderTypeParameterList(builder, typeConstructorParameters.subList(typeParameters.size, typeConstructorParameters.size))
            builder.append("*/")
        }
    }

    /* CLASSES */
    private fun renderClass(klass: ClassDescriptor, builder: StringBuilder) {
        val isEnumEntry = klass.kind == ClassKind.ENUM_ENTRY

        if (!startFromName) {
            builder.renderAnnotations(klass)
            if (!isEnumEntry) {
                renderVisibility(klass.visibility, builder)
            }
            if (!(klass.kind == ClassKind.INTERFACE && klass.modality == Modality.ABSTRACT ||
                        klass.kind.isSingleton && klass.modality == Modality.FINAL)
            ) {
                renderModality(klass.modality, builder, klass.implicitModalityWithoutExtensions())
            }
            renderMemberModifiers(klass, builder)
            renderModifier(builder, DescriptorRendererModifier.INNER in modifiers && klass.isInner, "inner")
            renderModifier(builder, DescriptorRendererModifier.DATA in modifiers && klass.isData, "data")
            renderModifier(builder, DescriptorRendererModifier.INLINE in modifiers && klass.isInline, "inline")
            renderModifier(builder, DescriptorRendererModifier.VALUE in modifiers && klass.isValue, "value")
            renderModifier(builder, DescriptorRendererModifier.FUN in modifiers && klass.isFun, "fun")
            renderClassKindPrefix(klass, builder)
        }

        if (!isCompanionObject(klass)) {
            if (!startFromName) renderSpaceIfNeeded(builder)
            renderName(klass, builder, true)
        } else {
            renderCompanionObjectName(klass, builder)
        }

        if (isEnumEntry) return

        val typeParameters = klass.declaredTypeParameters
        renderTypeParameters(typeParameters, builder, false)
        renderCapturedTypeParametersIfRequired(klass, builder)

        if (!klass.kind.isSingleton && classWithPrimaryConstructor) {
            val primaryConstructor = klass.unsubstitutedPrimaryConstructor
            if (primaryConstructor != null) {
                builder.append(" ")
                builder.renderAnnotations(primaryConstructor)
                renderVisibility(primaryConstructor.visibility, builder)
                builder.append(renderKeyword("constructor"))
                renderValueParameters(primaryConstructor.valueParameters, primaryConstructor.hasSynthesizedParameterNames(), builder)
            }
        }

        renderSuperTypes(klass, builder)
        renderWhereSuffix(typeParameters, builder)
    }

    private fun renderSuperTypes(klass: ClassDescriptor, builder: StringBuilder) {
        if (withoutSuperTypes) return

        if (KotlinBuiltIns.isNothing(klass.defaultType)) return

        val supertypes = klass.typeConstructor.supertypes
        if (supertypes.isEmpty() || supertypes.size == 1 && KotlinBuiltIns.isAnyOrNullableAny(supertypes.iterator().next())) return

        renderSpaceIfNeeded(builder)
        builder.append(": ")
        supertypes.joinTo(builder, ", ") { renderType(it) }
    }

    private fun renderClassKindPrefix(klass: ClassDescriptor, builder: StringBuilder) {
        builder.append(renderKeyword(getClassifierKindPrefix(klass)))
    }


    /* OTHER */
    private fun renderPackageView(packageView: PackageViewDescriptor, builder: StringBuilder) {
        renderPackageHeader(packageView.fqName, "package", builder)
        if (debugMode) {
            builder.append(" in context of ")
            renderName(packageView.module, builder, false)
        }
    }

    private fun renderPackageFragment(fragment: PackageFragmentDescriptor, builder: StringBuilder) {
        renderPackageHeader(fragment.fqName, "package-fragment", builder)
        if (debugMode) {
            builder.append(" in ")
            renderName(fragment.containingDeclaration, builder, false)
        }
    }

    private fun renderPackageHeader(fqName: FqName, fragmentOrView: String, builder: StringBuilder) {
        builder.append(renderKeyword(fragmentOrView))
        val fqNameString = renderFqName(fqName.toUnsafe())
        if (fqNameString.isNotEmpty()) {
            builder.append(" ")
            builder.append(fqNameString)
        }
    }

    private fun renderAccessorModifiers(descriptor: PropertyAccessorDescriptor, builder: StringBuilder) {
        renderMemberModifiers(descriptor, builder)
    }

    /* STUPID DISPATCH-ONLY VISITOR */
    private inner class RenderDeclarationDescriptorVisitor : DeclarationDescriptorVisitor<Unit, StringBuilder> {
        override fun visitValueParameterDescriptor(descriptor: ValueParameterDescriptor, builder: StringBuilder) {
            renderValueParameter(descriptor, true, builder, true)
        }

        override fun visitVariableDescriptor(descriptor: VariableDescriptor, builder: StringBuilder) {
            renderVariable(descriptor, true, builder, true)
        }

        override fun visitPropertyDescriptor(descriptor: PropertyDescriptor, builder: StringBuilder) {
            renderProperty(descriptor, builder)
        }

        override fun visitPropertyGetterDescriptor(descriptor: PropertyGetterDescriptor, builder: StringBuilder) {
            visitPropertyAccessorDescriptor(descriptor, builder, "getter")
        }

        override fun visitPropertySetterDescriptor(descriptor: PropertySetterDescriptor, builder: StringBuilder) {
            visitPropertyAccessorDescriptor(descriptor, builder, "setter")
        }

        private fun visitPropertyAccessorDescriptor(descriptor: PropertyAccessorDescriptor, builder: StringBuilder, kind: String) {
            when (propertyAccessorRenderingPolicy) {
                PropertyAccessorRenderingPolicy.PRETTY -> {
                    renderAccessorModifiers(descriptor, builder)
                    builder.append("$kind for ")
                    renderProperty(descriptor.correspondingProperty, builder)
                }
                PropertyAccessorRenderingPolicy.DEBUG -> {
                    visitFunctionDescriptor(descriptor, builder)
                }
                PropertyAccessorRenderingPolicy.NONE -> {
                }
            }
        }

        override fun visitFunctionDescriptor(descriptor: FunctionDescriptor, builder: StringBuilder) {
            renderFunction(descriptor, builder)
        }

        override fun visitReceiverParameterDescriptor(descriptor: ReceiverParameterDescriptor, builder: StringBuilder) {
            builder.append(descriptor.name) // renders <this>
        }

        override fun visitConstructorDescriptor(constructorDescriptor: ConstructorDescriptor, builder: StringBuilder) {
            renderConstructor(constructorDescriptor, builder)
        }

        override fun visitTypeParameterDescriptor(descriptor: TypeParameterDescriptor, builder: StringBuilder) {
            renderTypeParameter(descriptor, builder, true)
        }

        override fun visitPackageFragmentDescriptor(descriptor: PackageFragmentDescriptor, builder: StringBuilder) {
            renderPackageFragment(descriptor, builder)
        }

        override fun visitPackageViewDescriptor(descriptor: PackageViewDescriptor, builder: StringBuilder) {
            renderPackageView(descriptor, builder)
        }

        override fun visitModuleDeclaration(descriptor: ModuleDescriptor, builder: StringBuilder) {
            renderName(descriptor, builder, true)
        }

        override fun visitScriptDescriptor(scriptDescriptor: ScriptDescriptor, builder: StringBuilder) {
            visitClassDescriptor(scriptDescriptor, builder)
        }

        override fun visitClassDescriptor(descriptor: ClassDescriptor, builder: StringBuilder) {
            renderClass(descriptor, builder)
        }

        override fun visitTypeAliasDescriptor(descriptor: TypeAliasDescriptor, builder: StringBuilder) {
            renderTypeAlias(descriptor, builder)
        }
    }

    private fun renderSpaceIfNeeded(builder: StringBuilder) {
        val length = builder.length
        if (length == 0 || builder[length - 1] != ' ') {
            builder.append(' ')
        }
    }

    private fun replacePrefixes(
        lowerRendered: String,
        lowerPrefix: String,
        upperRendered: String,
        upperPrefix: String,
        foldedPrefix: String
    ): String? {
        if (lowerRendered.startsWith(lowerPrefix) && upperRendered.startsWith(upperPrefix)) {
            val lowerWithoutPrefix = lowerRendered.substring(lowerPrefix.length)
            val upperWithoutPrefix = upperRendered.substring(upperPrefix.length)
            val flexibleCollectionName = foldedPrefix + lowerWithoutPrefix

            if (lowerWithoutPrefix == upperWithoutPrefix) return flexibleCollectionName

            if (differsOnlyInNullability(lowerWithoutPrefix, upperWithoutPrefix)) {
                return "$flexibleCollectionName!"
            }
        }
        return null
    }

    private fun differsOnlyInNullability(lower: String, upper: String) =
        lower == upper.replace("?", "") || upper.endsWith("?") && ("$lower?") == upper || "($lower)?" == upper

    private fun overridesSomething(callable: CallableMemberDescriptor) = !callable.overriddenDescriptors.isEmpty()
}
