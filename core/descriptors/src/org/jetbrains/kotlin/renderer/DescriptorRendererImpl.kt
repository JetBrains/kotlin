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

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotated
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.DefaultAnnotationArgumentVisitor
import org.jetbrains.kotlin.descriptors.impl.DeclarationDescriptorVisitorEmptyBodies
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.FqNameBase
import org.jetbrains.kotlin.name.FqNameUnsafe
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.constants.*
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.ErrorUtils.UninferredParameterTypeConstructor
import org.jetbrains.kotlin.types.error.MissingDependencyErrorClass
import org.jetbrains.kotlin.utils.*

import java.util.*

import org.jetbrains.kotlin.resolve.DescriptorUtils.isCompanionObject
import org.jetbrains.kotlin.types.TypeUtils.CANT_INFER_FUNCTION_PARAM_TYPE

internal class DescriptorRendererImpl(
        val options: DescriptorRendererOptionsImpl
) : DescriptorRenderer, DescriptorRendererOptions by options/* this gives access to options without qualifier */ {

    init {
        assert(options.isLocked)
    }

    /* FORMATTING */
    private fun renderKeyword(keyword: String): String {
        when (textFormat) {
            DescriptorRenderer.TextFormat.PLAIN -> return keyword
            DescriptorRenderer.TextFormat.HTML -> return "<b>" + keyword + "</b>"
        }
    }

    private fun renderError(keyword: String): String {
        when (textFormat) {
            DescriptorRenderer.TextFormat.PLAIN -> return keyword
            DescriptorRenderer.TextFormat.HTML -> return "<font color=red><b>" + keyword + "</b></font>"
        }
    }

    private fun escape(string: String): String {
        when (textFormat) {
            DescriptorRenderer.TextFormat.PLAIN -> return string
            DescriptorRenderer.TextFormat.HTML -> return string.replace("<", "&lt;").replace(">", "&gt;")
        }
    }

    private fun lt(): String {
        return escape("<")
    }

    private fun gt(): String {
        return escape(">")
    }

    private fun arrow(): String {
        when (textFormat) {
            DescriptorRenderer.TextFormat.PLAIN -> return escape("->")
            DescriptorRenderer.TextFormat.HTML -> return "&rarr;"
        }
    }

    private fun renderMessage(message: String): String {
        when (textFormat) {
            DescriptorRenderer.TextFormat.PLAIN -> return message
            DescriptorRenderer.TextFormat.HTML -> return "<i>" + message + "</i>"
        }
    }

    /* NAMES RENDERING */
    override fun renderName(name: Name): String {
        val asString = name.asString()
        return escape(if (nameShouldBeEscaped(name)) '`' + asString + '`' else asString)
    }

    private fun renderName(descriptor: DeclarationDescriptor, builder: StringBuilder) {
        builder.append(renderName(descriptor.getName()))
    }

    private fun renderCompanionObjectName(descriptor: DeclarationDescriptor, builder: StringBuilder) {
        if (renderCompanionObjectName) {
            if (startFromName) {
                builder.append("companion object")
            }
            renderSpaceIfNeeded(builder)
            val containingDeclaration = descriptor.getContainingDeclaration()
            if (containingDeclaration != null) {
                builder.append("of ")
                builder.append(renderName(containingDeclaration.getName()))
            }
        }
        if (verbose) {
            if (!startFromName) renderSpaceIfNeeded(builder)
            builder.append(renderName(descriptor.getName()))
        }
    }

    override fun renderFqName(fqName: FqNameBase): String {
        return renderFqName(fqName.pathSegments())
    }

    private fun renderFqName(pathSegments: List<Name>): String {
        val buf = StringBuilder()
        for (element in pathSegments) {
            if (buf.length() != 0) {
                buf.append(".")
            }
            buf.append(renderName(element))
        }
        return buf.toString()
    }

    override fun renderClassifierName(klass: ClassifierDescriptor): String {
        if (klass is MissingDependencyErrorClass) {
            return klass.fullFqName.asString()
        }
        if (ErrorUtils.isError(klass)) {
            return klass.getTypeConstructor().toString()
        }
        when (nameShortness) {
            NameShortness.SHORT -> {
                val qualifiedNameElements = ArrayList<Name>()

                // for nested classes qualified name should be used
                var current: DeclarationDescriptor? = klass
                do {
                    qualifiedNameElements.add(current!!.getName())
                    current = current.getContainingDeclaration()
                }
                while (current is ClassDescriptor)

                return renderFqName(qualifiedNameElements.reverse())
            }

            NameShortness.FULLY_QUALIFIED -> return renderFqName(DescriptorUtils.getFqName(klass))

            NameShortness.SOURCE_CODE_QUALIFIED -> return qualifiedNameForSourceCode(klass)

            else -> throw IllegalArgumentException()
        }
    }

    /* TYPES RENDERING */
    override fun renderType(type: JetType): String {
        return renderNormalizedType(typeNormalizer.invoke(type))
    }

    private fun renderNormalizedType(type: JetType): String {
        if (type is LazyType && debugMode) {
            return type.toString()
        }
        if (type.isDynamic()) {
            return "dynamic"
        }
        if (type.isFlexible()) {
            if (debugMode) {
                return renderFlexibleTypeWithBothBounds(type.flexibility().lowerBound, type.flexibility().upperBound)
            }
            else if (flexibleTypesForCode) {
                val prefix = if (nameShortness == NameShortness.SHORT) "" else Flexibility.FLEXIBLE_TYPE_CLASSIFIER.getPackageFqName().asString() + "."
                return prefix + Flexibility.FLEXIBLE_TYPE_CLASSIFIER.getRelativeClassName() + lt() + renderNormalizedType(type.flexibility().lowerBound) + ", " + renderNormalizedType(type.flexibility().upperBound) + gt()
            }
            else {
                return renderFlexibleType(type)
            }
        }
        return renderInflexibleType(type)
    }

    private fun renderFlexibleTypeWithBothBounds(lower: JetType, upper: JetType): String {
        return "(" + renderNormalizedType(lower) + ".." + renderNormalizedType(upper) + ")"
    }

    private fun renderInflexibleType(type: JetType): String {
        assert(!type.isFlexible()) { "Flexible types not allowed here: " + renderNormalizedType(type) }

        if (type identityEquals CANT_INFER_FUNCTION_PARAM_TYPE || TypeUtils.isDontCarePlaceholder(type)) {
            return "???"
        }
        if (ErrorUtils.isUninferredParameter(type)) {
            if (uninferredTypeParameterAsName) {
                return renderError((type.getConstructor() as UninferredParameterTypeConstructor).getTypeParameterDescriptor().getName().toString())
            }
            return "???"
        }
        if (type.isError()) {
            return renderDefaultType(type)
        }
        if (shouldRenderAsPrettyFunctionType(type)) {
            return renderFunctionType(type)
        }
        return renderDefaultType(type)
    }

    private fun shouldRenderAsPrettyFunctionType(type: JetType): Boolean {
        return KotlinBuiltIns.isExactFunctionOrExtensionFunctionType(type) && prettyFunctionTypes
    }

    private fun renderFlexibleType(type: JetType): String {
        val lower = type.flexibility().lowerBound
        val upper = type.flexibility().upperBound

        val lowerRendered = renderInflexibleType(lower)
        val upperRendered = renderInflexibleType(upper)

        if (differsOnlyInNullability(lowerRendered, upperRendered)) {
            if (upperRendered.startsWith("(")) {
                // the case of complex type, e.g. (() -> Unit)?
                return "(" + lowerRendered + ")!"
            }
            return lowerRendered + "!"
        }

        val kotlinPrefix = if (nameShortness != NameShortness.SHORT) "kotlin." else ""
        val mutablePrefix = "Mutable"
        // java.util.List<Foo> -> (Mutable)List<Foo!>!
        val simpleCollection = replacePrefixes(lowerRendered, kotlinPrefix + mutablePrefix, upperRendered, kotlinPrefix, kotlinPrefix + "(" + mutablePrefix + ")")
        if (simpleCollection != null) return simpleCollection
        // java.util.Map.Entry<Foo, Bar> -> (Mutable)Map.(Mutable)Entry<Foo!, Bar!>!
        val mutableEntry = replacePrefixes(lowerRendered, kotlinPrefix + "MutableMap.MutableEntry", upperRendered, kotlinPrefix + "Map.Entry", kotlinPrefix + "(Mutable)Map.(Mutable)Entry")
        if (mutableEntry != null) return mutableEntry

        // Foo[] -> Array<(out) Foo!>!
        val array = replacePrefixes(lowerRendered, kotlinPrefix + escape("Array<"), upperRendered, kotlinPrefix + escape("Array<out "), kotlinPrefix + escape("Array<(out) "))
        if (array != null) return array
        return renderFlexibleTypeWithBothBounds(lower, upper)
    }

    override fun renderTypeArguments(typeArguments: List<TypeProjection>): String {
        if (typeArguments.isEmpty()) return ""
        val sb = StringBuilder()
        sb.append(lt())
        appendTypeProjections(typeArguments, sb)
        sb.append(gt())
        return sb.toString()
    }

    private fun renderDefaultType(type: JetType): String {
        val sb = StringBuilder()

        renderAnnotations(type, sb, /* needBrackets = */ true)

        if (type.isError()) {
            sb.append(type.getConstructor().toString()) // Debug name of an error type is more informative
        }
        else {
            sb.append(renderTypeName(type.getConstructor()))
        }
        sb.append(renderTypeArguments(type.getArguments()))
        if (type.isMarkedNullable()) {
            sb.append("?")
        }
        return sb.toString()
    }

    private fun renderTypeName(typeConstructor: TypeConstructor): String {
        val cd = typeConstructor.getDeclarationDescriptor()
        if (cd is TypeParameterDescriptor) {
            return renderName(cd.getName())
        }
        else if (cd is ClassDescriptor) {
            return renderClassifierName(cd)
        }
        else {
            assert(cd == null) { "Unexpected classifier: " + cd!!.javaClass }
            return typeConstructor.toString()
        }
    }

    private fun appendTypeProjections(typeProjections: List<TypeProjection>, builder: StringBuilder) {
        val iterator = typeProjections.iterator()
        while (iterator.hasNext()) {
            val typeProjection = iterator.next()
            if (typeProjection.isStarProjection()) {
                builder.append("*")
            }
            else {
                if (typeProjection.getProjectionKind() != Variance.INVARIANT) {
                    builder.append(typeProjection.getProjectionKind()).append(" ")
                }
                builder.append(renderType(typeProjection.getType()))
            }
            if (iterator.hasNext()) {
                builder.append(", ")
            }
        }
    }

    private fun renderFunctionType(type: JetType): String {
        val sb = StringBuilder()

        val receiverType = KotlinBuiltIns.getReceiverType(type)
        if (receiverType != null) {
            sb.append(renderNormalizedType(receiverType))
            sb.append(".")
        }

        sb.append("(")
        appendTypeProjections(KotlinBuiltIns.getParameterTypeProjectionsFromFunctionType(type), sb)
        sb.append(") ").append(arrow()).append(" ")
        sb.append(renderNormalizedType(KotlinBuiltIns.getReturnTypeFromFunctionType(type)))

        if (type.isMarkedNullable()) {
            return "(" + sb + ")?"
        }
        return sb.toString()
    }


    /* METHODS FOR ALL KINDS OF DESCRIPTORS */
    private fun appendDefinedIn(descriptor: DeclarationDescriptor, builder: StringBuilder) {
        if (descriptor is PackageFragmentDescriptor || descriptor is PackageViewDescriptor) {
            return
        }
        if (descriptor is ModuleDescriptor) {
            builder.append(" is a module")
            return
        }
        builder.append(" ").append(renderMessage("defined in")).append(" ")

        val containingDeclaration = descriptor.getContainingDeclaration()
        if (containingDeclaration != null) {
            val fqName = DescriptorUtils.getFqName(containingDeclaration)
            builder.append(if (FqName.ROOT.equalsTo(fqName)) "root package" else renderFqName(fqName))
        }
    }

    private fun renderAnnotations(annotated: Annotated, builder: StringBuilder, needBrackets: Boolean = false) {
        if (!modifiers.contains(DescriptorRenderer.Modifier.ANNOTATIONS)) return

        val excluded = if (annotated is JetType) excludedTypeAnnotationClasses else excludedAnnotationClasses

        val annotationsBuilder = StringBuilder()
        for (annotation in annotated.getAnnotations()) {
            val annotationClass = annotation.getType().getConstructor().getDeclarationDescriptor() as ClassDescriptor

            if (!excluded.contains(DescriptorUtils.getFqNameSafe(annotationClass))) {
                annotationsBuilder.append(renderAnnotation(annotation)).append(" ")
            }
        }

        if (!needBrackets) {
            builder.append(annotationsBuilder)
        }
        else if (annotationsBuilder.length() > 0) {
            // remove last whitespace
            annotationsBuilder.setLength(annotationsBuilder.length() - 1)

            builder.append("@[")
            builder.append(annotationsBuilder)
            builder.append("] ")
        }
    }

    override fun renderAnnotation(annotation: AnnotationDescriptor): String {
        val sb = StringBuilder()
        sb.append(renderType(annotation.getType()))
        if (verbose) {
            renderAndSortAnnotationArguments(annotation).joinTo(sb, ", ", "(", ")")
        }
        return sb.toString()
    }

    private fun renderAndSortAnnotationArguments(descriptor: AnnotationDescriptor): List<String> {
        val valueArguments = descriptor.getAllValueArguments().entrySet()
        val resultList = ArrayList<String>(valueArguments.size())
        for (entry in valueArguments) {
            val value = entry.getValue()
            val typeSuffix = ": " + renderType(value.getType(KotlinBuiltIns.getInstance()))
            resultList.add(entry.getKey().getName().asString() + " = " + renderConstant(value) + typeSuffix)
        }
        Collections.sort(resultList)
        return resultList
    }

    private fun renderConstant(value: CompileTimeConstant<*>): String {
        return value.accept(object : DefaultAnnotationArgumentVisitor<String, Unit>() {
            override fun visitValue(value: CompileTimeConstant<*>, data: Unit): String {
                return value.toString()
            }

            override fun visitArrayValue(value: ArrayValue, data: Unit): String {
                return value.getValue().map { renderConstant(it) }.joinToString(", ", "{", "}")
            }

            override fun visitAnnotationValue(value: AnnotationValue, data: Unit): String {
                return renderAnnotation(value.getValue())
            }

            override fun visitJavaClassValue(value: JavaClassValue, data: Unit): String {
                return "javaClass<" + renderType(value.getValue()) + ">()"
            }

            override fun visitKClassValue(value: KClassValue, data: Unit): String {
                return renderType(value.getValue()) + "::class"
            }
        }, Unit)
    }

    private fun renderVisibility(visibility: Visibility, builder: StringBuilder) {
        var visibility = visibility
        if (!modifiers.contains(DescriptorRenderer.Modifier.VISIBILITY)) return
        if (normalizedVisibilities) {
            visibility = visibility.normalize()
        }
        if (!showInternalKeyword && visibility == Visibilities.INTERNAL) return
        builder.append(renderKeyword(visibility.toString())).append(" ")
    }

    private fun renderModality(modality: Modality, builder: StringBuilder) {
        if (!modifiers.contains(DescriptorRenderer.Modifier.MODALITY)) return
        val keyword = modality.name().toLowerCase()
        builder.append(renderKeyword(keyword)).append(" ")
    }

    private fun renderInner(isInner: Boolean, builder: StringBuilder) {
        if (!modifiers.contains(DescriptorRenderer.Modifier.INNER)) return
        if (isInner) {
            builder.append(renderKeyword("inner")).append(" ")
        }
    }

    private fun renderModalityForCallable(callable: CallableMemberDescriptor, builder: StringBuilder) {
        if (!DescriptorUtils.isTopLevelDeclaration(callable) || callable.getModality() != Modality.FINAL) {
            if (overridesSomething(callable) && overrideRenderingPolicy == DescriptorRenderer.OverrideRenderingPolicy.RENDER_OVERRIDE && callable.getModality() == Modality.OPEN) {
                return
            }
            renderModality(callable.getModality(), builder)
        }
    }

    private fun renderOverride(callableMember: CallableMemberDescriptor, builder: StringBuilder) {
        if (!modifiers.contains(DescriptorRenderer.Modifier.OVERRIDE)) return
        if (overridesSomething(callableMember)) {
            if (overrideRenderingPolicy != DescriptorRenderer.OverrideRenderingPolicy.RENDER_OPEN) {
                builder.append("override ")
                if (verbose) {
                    builder.append("/*").append(callableMember.getOverriddenDescriptors().size()).append("*/ ")
                }
            }
        }
    }

    private fun renderMemberKind(callableMember: CallableMemberDescriptor, builder: StringBuilder) {
        if (!modifiers.contains(DescriptorRenderer.Modifier.MEMBER_KIND)) return
        if (verbose && callableMember.getKind() != CallableMemberDescriptor.Kind.DECLARATION) {
            builder.append("/*").append(callableMember.getKind().name().toLowerCase()).append("*/ ")
        }
    }

    override fun render(declarationDescriptor: DeclarationDescriptor): String {
        val stringBuilder = StringBuilder()
        declarationDescriptor.accept(RenderDeclarationDescriptorVisitor(), stringBuilder)

        if (withDefinedIn) {
            appendDefinedIn(declarationDescriptor, stringBuilder)
        }
        return stringBuilder.toString()
    }


    /* TYPE PARAMETERS */
    private fun renderTypeParameter(typeParameter: TypeParameterDescriptor, builder: StringBuilder, topLevel: Boolean) {
        if (topLevel) {
            builder.append(lt())
        }

        if (verbose) {
            builder.append("/*").append(typeParameter.getIndex()).append("*/ ")
        }

        if (typeParameter.isReified()) {
            builder.append(renderKeyword("reified")).append(" ")
        }
        val variance = typeParameter.getVariance().label
        if (!variance.isEmpty()) {
            builder.append(renderKeyword(variance)).append(" ")
        }
        renderName(typeParameter, builder)
        val upperBoundsCount = typeParameter.getUpperBounds().size()
        if ((upperBoundsCount > 1 && !topLevel) || upperBoundsCount == 1) {
            val upperBound = typeParameter.getUpperBounds().iterator().next()
            if (KotlinBuiltIns.getInstance().getDefaultBound() != upperBound) {
                builder.append(" : ").append(renderType(upperBound))
            }
        }
        else if (topLevel) {
            var first = true
            for (upperBound in typeParameter.getUpperBounds()) {
                if (upperBound == KotlinBuiltIns.getInstance().getDefaultBound()) {
                    continue
                }
                if (first) {
                    builder.append(" : ")
                }
                else {
                    builder.append(" & ")
                }
                builder.append(renderType(upperBound))
                first = false
            }
        }
        else {
            // rendered with "where"
        }

        if (topLevel) {
            builder.append(gt())
        }
    }

    private fun renderTypeParameters(typeParameters: List<TypeParameterDescriptor>, builder: StringBuilder, withSpace: Boolean) {
        if (withoutTypeParameters) return

        if (!typeParameters.isEmpty()) {
            builder.append(lt())
            val iterator = typeParameters.iterator()
            while (iterator.hasNext()) {
                val typeParameterDescriptor = iterator.next()
                renderTypeParameter(typeParameterDescriptor, builder, false)
                if (iterator.hasNext()) {
                    builder.append(", ")
                }
            }
            builder.append(gt())
            if (withSpace) {
                builder.append(" ")
            }
        }
    }

    /* FUNCTIONS */
    private fun renderFunction(function: FunctionDescriptor, builder: StringBuilder) {
        if (!startFromName) {
            renderAnnotations(function, builder)
            renderVisibility(function.getVisibility(), builder)
            renderModalityForCallable(function, builder)
            renderOverride(function, builder)
            renderMemberKind(function, builder)

            builder.append(renderKeyword("fun")).append(" ")
            renderTypeParameters(function.getTypeParameters(), builder, true)
            renderReceiver(function, builder)
        }

        renderName(function, builder)

        renderValueParameters(function, builder)

        renderReceiverAfterName(function, builder)

        val returnType = function.getReturnType()
        if (unitReturnType || (returnType == null || !KotlinBuiltIns.isUnit(returnType))) {
            builder.append(": ").append(if (returnType == null) "[NULL]" else escape(renderType(returnType)))
        }

        renderWhereSuffix(function.getTypeParameters(), builder)
    }

    private fun renderReceiverAfterName(callableDescriptor: CallableDescriptor, builder: StringBuilder) {
        if (!receiverAfterName) return

        val receiver = callableDescriptor.getExtensionReceiverParameter()
        if (receiver != null) {
            builder.append(" on ").append(escape(renderType(receiver.getType())))
        }
    }

    private fun renderReceiver(callableDescriptor: CallableDescriptor, builder: StringBuilder) {
        val receiver = callableDescriptor.getExtensionReceiverParameter()
        if (receiver != null) {
            val type = receiver.getType()
            var result = escape(renderType(type))
            if (shouldRenderAsPrettyFunctionType(type) && !TypeUtils.isNullableType(type)) {
                result = "(" + result + ")"
            }
            builder.append(result).append(".")
        }
    }

    private fun renderConstructor(constructor: ConstructorDescriptor, builder: StringBuilder) {
        renderAnnotations(constructor, builder)
        renderVisibility(constructor.getVisibility(), builder)
        renderMemberKind(constructor, builder)

        builder.append(renderKeyword("constructor"))
        if (secondaryConstructorsAsPrimary) {
            val classDescriptor = constructor.getContainingDeclaration()
            builder.append(" ")
            renderName(classDescriptor, builder)
            renderTypeParameters(classDescriptor.getTypeConstructor().getParameters(), builder, false)
        }

        renderValueParameters(constructor, builder)

        if (secondaryConstructorsAsPrimary) {
            renderWhereSuffix(constructor.getTypeParameters(), builder)
        }
    }

    private fun renderWhereSuffix(typeParameters: List<TypeParameterDescriptor>, builder: StringBuilder) {
        if (withoutTypeParameters) return

        val upperBoundStrings = ArrayList<String>(0)

        for (typeParameter in typeParameters) {
            if (typeParameter.getUpperBounds().size() > 1) {
                var first = true
                for (upperBound in typeParameter.getUpperBounds()) {
                    // first parameter is rendered by renderTypeParameter:
                    if (!first) {
                        upperBoundStrings.add(renderName(typeParameter.getName()) + " : " + escape(renderType(upperBound)))
                    }
                    first = false
                }
            }
        }
        if (!upperBoundStrings.isEmpty()) {
            builder.append(" ").append(renderKeyword("where")).append(" ")
            upperBoundStrings.joinTo(builder, ", ")
        }
    }

    override fun renderFunctionParameters(functionDescriptor: FunctionDescriptor): String {
        val stringBuilder = StringBuilder()
        renderValueParameters(functionDescriptor, stringBuilder)
        return stringBuilder.toString()
    }

    private fun renderValueParameters(function: FunctionDescriptor, builder: StringBuilder) {
        val includeNames = shouldRenderParameterNames(function)
        valueParametersHandler.appendBeforeValueParameters(function, builder)
        for (parameter in function.getValueParameters()) {
            valueParametersHandler.appendBeforeValueParameter(parameter, builder)
            renderValueParameter(parameter, includeNames, builder, false)
            valueParametersHandler.appendAfterValueParameter(parameter, builder)
        }
        valueParametersHandler.appendAfterValueParameters(function, builder)
    }

    private fun shouldRenderParameterNames(function: FunctionDescriptor): Boolean {
        when (parameterNameRenderingPolicy) {
            DescriptorRenderer.ParameterNameRenderingPolicy.ALL -> return true
            DescriptorRenderer.ParameterNameRenderingPolicy.ONLY_NON_SYNTHESIZED -> return !function.hasSynthesizedParameterNames()
            DescriptorRenderer.ParameterNameRenderingPolicy.NONE -> return false
            else -> throw UnsupportedOperationException(parameterNameRenderingPolicy.toString())
        }
    }

    /* VARIABLES */
    private fun renderValueParameter(valueParameter: ValueParameterDescriptor, includeName: Boolean, builder: StringBuilder, topLevel: Boolean) {
        if (topLevel) {
            builder.append(renderKeyword("value-parameter")).append(" ")
        }

        if (verbose) {
            builder.append("/*").append(valueParameter.getIndex()).append("*/ ")
        }

        renderAnnotations(valueParameter, builder)
        renderVariable(valueParameter, includeName, builder, topLevel)
        val withDefaultValue = renderDefaultValues && (if (debugMode) valueParameter.declaresDefaultValue() else valueParameter.hasDefaultValue())
        if (withDefaultValue) {
            builder.append(" = ...")
        }
    }

    private fun renderValVarPrefix(variable: VariableDescriptor, builder: StringBuilder) {
        builder.append(renderKeyword(if (variable.isVar()) "var" else "val")).append(" ")
    }

    private fun renderVariable(variable: VariableDescriptor, includeName: Boolean, builder: StringBuilder, topLevel: Boolean) {
        val realType = variable.getType()

        val varargElementType = if (variable is ValueParameterDescriptor)
            variable.getVarargElementType()
        else
            null
        val typeToRender = varargElementType ?: realType

        if (varargElementType != null) {
            builder.append(renderKeyword("vararg")).append(" ")
        }
        if (topLevel && !startFromName) {
            renderValVarPrefix(variable, builder)
        }

        if (includeName) {
            renderName(variable, builder)
            builder.append(": ")
        }

        builder.append(escape(renderType(typeToRender)))

        renderInitializer(variable, builder)

        if (verbose && varargElementType != null) {
            builder.append(" /*").append(escape(renderType(realType))).append("*/")
        }
    }

    private fun renderProperty(property: PropertyDescriptor, builder: StringBuilder) {
        if (!startFromName) {
            renderAnnotations(property, builder)
            renderVisibility(property.getVisibility(), builder)
            renderModalityForCallable(property, builder)
            renderOverride(property, builder)
            renderMemberKind(property, builder)
            renderValVarPrefix(property, builder)
            renderTypeParameters(property.getTypeParameters(), builder, true)
            renderReceiver(property, builder)
        }

        renderName(property, builder)
        builder.append(": ").append(escape(renderType(property.getType())))

        renderReceiverAfterName(property, builder)

        renderInitializer(property, builder)

        renderWhereSuffix(property.getTypeParameters(), builder)
    }

    private fun renderInitializer(variable: VariableDescriptor, builder: StringBuilder) {
        if (includePropertyConstant) {
            val initializer = variable.getCompileTimeInitializer()
            if (initializer != null) {
                builder.append(" = ").append(escape(renderConstant(initializer)))
            }
        }
    }

    /* CLASSES */
    private fun renderClass(klass: ClassDescriptor, builder: StringBuilder) {
        if (!startFromName) {
            renderAnnotations(klass, builder)
            renderVisibility(klass.getVisibility(), builder)
            if (!(klass.getKind() == ClassKind.INTERFACE && klass.getModality() == Modality.ABSTRACT || klass.getKind().isSingleton() && klass.getModality() == Modality.FINAL)) {
                renderModality(klass.getModality(), builder)
            }
            renderInner(klass.isInner(), builder)
            renderClassKindPrefix(klass, builder)
        }

        if (!isCompanionObject(klass)) {
            if (!startFromName) renderSpaceIfNeeded(builder)
            renderName(klass, builder)
        }
        else {
            renderCompanionObjectName(klass, builder)
        }

        val typeParameters = klass.getTypeConstructor().getParameters()
        renderTypeParameters(typeParameters, builder, false)

        if (!klass.getKind().isSingleton() && classWithPrimaryConstructor) {
            val primaryConstructor = klass.getUnsubstitutedPrimaryConstructor()
            if (primaryConstructor != null) {
                builder.append(" ")
                renderAnnotations(primaryConstructor, builder, true)
                renderVisibility(primaryConstructor.getVisibility(), builder)
                builder.append("constructor")
                renderValueParameters(primaryConstructor, builder)
            }
        }

        renderSuperTypes(klass, builder)
        renderWhereSuffix(typeParameters, builder)
    }

    private fun renderSuperTypes(klass: ClassDescriptor, builder: StringBuilder) {
        if (withoutSuperTypes) return

        if (klass != KotlinBuiltIns.getInstance().getNothing()) {
            val supertypes = klass.getTypeConstructor().getSupertypes()

            if (supertypes.isEmpty() || supertypes.size() == 1 && KotlinBuiltIns.isAnyOrNullableAny(supertypes.iterator().next())) {
            }
            else {
                renderSpaceIfNeeded(builder)
                builder.append(": ")
                val iterator = supertypes.iterator()
                while (iterator.hasNext()) {
                    val supertype = iterator.next()
                    builder.append(renderType(supertype))
                    if (iterator.hasNext()) {
                        builder.append(", ")
                    }
                }
            }
        }
    }

    private fun renderClassKindPrefix(klass: ClassDescriptor, builder: StringBuilder) {
        builder.append(renderKeyword(DescriptorRenderer.getClassKindPrefix(klass)))
    }


    /* OTHER */
    private fun renderModuleOrScript(moduleOrScript: DeclarationDescriptor, builder: StringBuilder) {
        renderName(moduleOrScript, builder)
    }

    private fun renderPackageView(packageView: PackageViewDescriptor, builder: StringBuilder) {
        builder.append(renderKeyword("package")).append(" ")
        builder.append(renderFqName(packageView.getFqName()))
        if (debugMode) {
            builder.append(" in context of ")
            renderName(packageView.getModule(), builder)
        }
    }

    private fun renderPackageFragment(fragment: PackageFragmentDescriptor, builder: StringBuilder) {
        builder.append(renderKeyword("package-fragment")).append(" ")
        builder.append(renderFqName(fragment.fqName))
        if (debugMode) {
            builder.append(" in ")
            renderName(fragment.getContainingDeclaration(), builder)
        }
    }


    /* STUPID DISPATCH-ONLY VISITOR */
    private inner class RenderDeclarationDescriptorVisitor : DeclarationDescriptorVisitorEmptyBodies<Void, StringBuilder>() {
        override fun visitValueParameterDescriptor(descriptor: ValueParameterDescriptor, builder: StringBuilder): Void? {
            renderValueParameter(descriptor, true, builder, true)
            return null
        }

        override fun visitVariableDescriptor(descriptor: VariableDescriptor, builder: StringBuilder): Void? {
            renderVariable(descriptor, true, builder, true)
            return null
        }

        override fun visitPropertyDescriptor(descriptor: PropertyDescriptor, builder: StringBuilder): Void? {
            renderProperty(descriptor, builder)
            return null
        }

        override fun visitPropertyGetterDescriptor(descriptor: PropertyGetterDescriptor, builder: StringBuilder): Void? {
            if (renderAccessors) {
                builder.append("getter for ")
                renderProperty(descriptor.getCorrespondingProperty(), builder)
                return null
            }
            else {
                return super.visitPropertyGetterDescriptor(descriptor, builder)
            }

        }

        override fun visitPropertySetterDescriptor(descriptor: PropertySetterDescriptor, builder: StringBuilder): Void? {
            if (renderAccessors) {
                builder.append("setter for ")
                renderProperty(descriptor.getCorrespondingProperty(), builder)
                return null
            }
            else {
                return super.visitPropertySetterDescriptor(descriptor, builder)
            }
        }

        override fun visitFunctionDescriptor(descriptor: FunctionDescriptor, builder: StringBuilder): Void? {
            renderFunction(descriptor, builder)
            return null
        }

        override fun visitReceiverParameterDescriptor(descriptor: ReceiverParameterDescriptor, data: StringBuilder): Void {
            throw UnsupportedOperationException("Don't render receiver parameters")
        }

        override fun visitConstructorDescriptor(constructorDescriptor: ConstructorDescriptor, builder: StringBuilder): Void? {
            renderConstructor(constructorDescriptor, builder)
            return null
        }

        override fun visitTypeParameterDescriptor(descriptor: TypeParameterDescriptor, builder: StringBuilder): Void? {
            renderTypeParameter(descriptor, builder, true)
            return null
        }

        override fun visitPackageFragmentDescriptor(descriptor: PackageFragmentDescriptor, builder: StringBuilder): Void? {
            renderPackageFragment(descriptor, builder)
            return null
        }

        override fun visitPackageViewDescriptor(descriptor: PackageViewDescriptor, builder: StringBuilder): Void? {
            renderPackageView(descriptor, builder)
            return null
        }

        override fun visitModuleDeclaration(descriptor: ModuleDescriptor, builder: StringBuilder): Void? {
            renderModuleOrScript(descriptor, builder)
            return null
        }

        override fun visitScriptDescriptor(scriptDescriptor: ScriptDescriptor, builder: StringBuilder): Void? {
            renderModuleOrScript(scriptDescriptor, builder)
            return null
        }

        override fun visitClassDescriptor(descriptor: ClassDescriptor, builder: StringBuilder): Void? {
            renderClass(descriptor, builder)
            return null
        }
    }

    private fun renderSpaceIfNeeded(builder: StringBuilder) {
        val length = builder.length()
        if (length == 0 || builder.charAt(length - 1) != ' ') {
            builder.append(' ')
        }
    }

    private fun nameShouldBeEscaped(identifier: Name): Boolean {
        if (identifier.isSpecial()) return false

        val name = identifier.asString()

        if (KeywordStringsGenerated.KEYWORDS.contains(name)) return true

        for (i in 0..name.length() - 1) {
            val c = name.charAt(i)
            if (!Character.isLetterOrDigit(c) && c != '_') return true
        }

        return false
    }

    private fun replacePrefixes(lowerRendered: String, lowerPrefix: String, upperRendered: String, upperPrefix: String, foldedPrefix: String): String? {
        if (lowerRendered.startsWith(lowerPrefix) && upperRendered.startsWith(upperPrefix)) {
            val lowerWithoutPrefix = lowerRendered.substring(lowerPrefix.length())
            if (differsOnlyInNullability(lowerWithoutPrefix, upperRendered.substring(upperPrefix.length()))) {
                return foldedPrefix + lowerWithoutPrefix + "!"
            }
        }
        return null
    }

    private fun differsOnlyInNullability(lower: String, upper: String)
            = lower == upper.replace("?", "") || upper.endsWith("?") && ("$lower?") == upper || "($lower)?" == upper

    private fun overridesSomething(callable: CallableMemberDescriptor) = !callable.getOverriddenDescriptors().isEmpty()
}
