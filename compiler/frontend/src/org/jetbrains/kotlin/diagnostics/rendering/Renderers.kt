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

package org.jetbrains.kotlin.diagnostics.rendering

import com.google.common.collect.Lists
import com.google.common.collect.Sets
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.cfg.WhenMissingCase
import org.jetbrains.kotlin.cfg.hasUnknown
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.TypeAliasConstructorDescriptor
import org.jetbrains.kotlin.diagnostics.rendering.TabledDescriptorRenderer.newTable
import org.jetbrains.kotlin.diagnostics.rendering.TabledDescriptorRenderer.newText
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.MemberComparator
import org.jetbrains.kotlin.resolve.MultiTargetPlatform
import org.jetbrains.kotlin.resolve.calls.inference.*
import org.jetbrains.kotlin.resolve.calls.inference.TypeBounds.Bound
import org.jetbrains.kotlin.resolve.calls.inference.TypeBounds.BoundKind.LOWER_BOUND
import org.jetbrains.kotlin.resolve.calls.inference.TypeBounds.BoundKind.UPPER_BOUND
import org.jetbrains.kotlin.resolve.calls.inference.constraintPosition.ConstraintPosition
import org.jetbrains.kotlin.resolve.calls.inference.constraintPosition.ConstraintPositionKind.*
import org.jetbrains.kotlin.resolve.calls.inference.constraintPosition.getValidityConstraintForConstituentType
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.getMultiTargetPlatform
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import java.io.PrintWriter
import java.io.StringWriter
import java.lang.AssertionError

object Renderers {

    private val LOG = Logger.getInstance(Renderers::class.java)

    @JvmField val TO_STRING = Renderer<Any> {
        element ->
        if (element is DeclarationDescriptor) {
            LOG.warn("Diagnostic renderer TO_STRING was used to render an instance of DeclarationDescriptor.\n"
                     + "This is usually a bad idea, because descriptors' toString() includes some debug information, "
                     + "which should not be seen by the user.\nDescriptor: " + element)
        }
        element.toString()
    }

    @JvmField val STRING = Renderer<String> { it }

    @JvmField val THROWABLE = Renderer<Throwable> {
        val writer = StringWriter()
        it.printStackTrace(PrintWriter(writer))
        StringUtil.first(writer.toString(), 2048, true)
    }

    @JvmField val NAME = Renderer<Named> { it.name.asString() }

    @JvmField val PLATFORM = Renderer<ModuleDescriptor> {
        val platform = it.getMultiTargetPlatform()
        " ${it.getCapability(ModuleInfo.Capability)?.displayedName ?: ""}" + when (platform) {
            MultiTargetPlatform.Common -> ""
            is MultiTargetPlatform.Specific -> " for " + platform.platform
            null -> ""
        }
    }

    @JvmField val VISIBILITY = Renderer<Visibility> {
        if (it == Visibilities.INVISIBLE_FAKE)
            "invisible (private in a supertype)"
        else it.displayName
    }

    @JvmField val DECLARATION_NAME_WITH_KIND = Renderer<DeclarationDescriptor> {
        val declarationKindWithSpace = when (it) {
            is PackageFragmentDescriptor -> "package "
            is ClassDescriptor -> "${it.renderKind()} "
            is TypeAliasDescriptor -> "typealias "
            is ConstructorDescriptor -> "constructor "
            is TypeAliasConstructorDescriptor -> "typealias constructor "
            is PropertyGetterDescriptor -> "property getter "
            is PropertySetterDescriptor -> "property setter "
            is FunctionDescriptor -> "function "
            is PropertyDescriptor -> "property "
            else -> throw AssertionError("Unexpected declaration kind: $it")
        }
        "$declarationKindWithSpace'${it.name.asString()}'"
    }

    @JvmField val NAME_OF_PARENT_OR_FILE = Renderer<DeclarationDescriptor> {
        if (DescriptorUtils.isTopLevelDeclaration(it) && it is DeclarationDescriptorWithVisibility && it.visibility == Visibilities.PRIVATE) {
            "file"
        }
        else {
            "'" + it.containingDeclaration!!.name + "'"
        }
    }

    @JvmField val ELEMENT_TEXT = Renderer<PsiElement> { it.text }

    @JvmField val DECLARATION_NAME = Renderer<KtNamedDeclaration> { it.nameAsSafeName.asString() }

    @JvmField val RENDER_CLASS_OR_OBJECT = Renderer {
        classOrObject: KtClassOrObject ->
        val name = if (classOrObject.name != null) " '" + classOrObject.name + "'" else ""
        if (classOrObject is KtClass) "Class" + name else "Object" + name
    }

    @JvmField val RENDER_CLASS_OR_OBJECT_NAME = Renderer<ClassifierDescriptorWithTypeParameters> { it.renderKindWithName() }

    @JvmField val RENDER_TYPE = SmartTypeRenderer(DescriptorRenderer.FQ_NAMES_IN_TYPES.withOptions { parameterNamesInFunctionalTypes = false })

    @JvmField val RENDER_POSITION_VARIANCE = Renderer {
        variance: Variance ->
        when (variance) {
            Variance.INVARIANT -> "invariant"
            Variance.IN_VARIANCE -> "in"
            Variance.OUT_VARIANCE -> "out"
        }
    }

    @JvmField val AMBIGUOUS_CALLS = Renderer {
        calls: Collection<ResolvedCall<*>> ->
        val descriptors = calls.map { it.resultingDescriptor }
        val context = RenderingContext.Impl(descriptors)
        descriptors
                .sortedWith(MemberComparator.INSTANCE)
                .joinToString(separator = "\n", prefix = "\n") { FQ_NAMES_IN_TYPES.render(it, context) }
    }

    @JvmStatic fun <T> commaSeparated(itemRenderer: DiagnosticParameterRenderer<T>) = ContextDependentRenderer<Collection<T>> {
        collection, context ->
        buildString {
            val iterator = collection.iterator()
            while (iterator.hasNext()) {
                val next = iterator.next()
                append(itemRenderer.render(next, context))
                if (iterator.hasNext()) {
                    append(", ")
                }
            }
        }
    }

    @JvmField val TYPE_INFERENCE_CONFLICTING_SUBSTITUTIONS_RENDERER = Renderer<InferenceErrorData> {
        renderConflictingSubstitutionsInferenceError(it, TabledDescriptorRenderer.create()).toString()
    }

    @JvmField val TYPE_INFERENCE_PARAMETER_CONSTRAINT_ERROR_RENDERER = Renderer<InferenceErrorData> {
        renderParameterConstraintError(it, TabledDescriptorRenderer.create()).toString()
    }

    @JvmField val TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER_RENDERER = Renderer<InferenceErrorData> {
        renderNoInformationForParameterError(it, TabledDescriptorRenderer.create()).toString()
    }

    @JvmField val TYPE_INFERENCE_UPPER_BOUND_VIOLATED_RENDERER = Renderer<InferenceErrorData> {
        renderUpperBoundViolatedInferenceError(it, TabledDescriptorRenderer.create()).toString()
    }

    @JvmField val TYPE_INFERENCE_CANNOT_CAPTURE_TYPES_RENDERER = Renderer<InferenceErrorData> {
        renderCannotCaptureTypeParameterError(it, TabledDescriptorRenderer.create()).toString()
    }

    @JvmStatic fun renderConflictingSubstitutionsInferenceError(
            inferenceErrorData: InferenceErrorData, result: TabledDescriptorRenderer
    ): TabledDescriptorRenderer {
        LOG.assertTrue(inferenceErrorData.constraintSystem.status.hasConflictingConstraints(),
                       debugMessage("Conflicting substitutions inference error renderer is applied for incorrect status", inferenceErrorData))

        val substitutedDescriptors = Lists.newArrayList<CallableDescriptor>()
        val substitutors = ConstraintsUtil.getSubstitutorsForConflictingParameters(inferenceErrorData.constraintSystem)
        for (substitutor in substitutors) {
            val substitutedDescriptor = inferenceErrorData.descriptor.substitute(substitutor)
            substitutedDescriptors.add(substitutedDescriptor)
        }

        val firstConflictingVariable = ConstraintsUtil.getFirstConflictingVariable(inferenceErrorData.constraintSystem)
        if (firstConflictingVariable == null) {
            LOG.error(debugMessage("There is no conflicting parameter for 'conflicting constraints' error.", inferenceErrorData))
            return result
        }

        result.text(newText()
                            .normal("Cannot infer type parameter ")
                            .strong(firstConflictingVariable.name)
                            .normal(" in "))
        val table = newTable()
        result.table(table)
        table.descriptor(inferenceErrorData.descriptor).text("None of the following substitutions")

        for (substitutedDescriptor in substitutedDescriptors) {
            val receiverType = DescriptorUtils.getReceiverParameterType(substitutedDescriptor.extensionReceiverParameter)

            val errorPositions = Sets.newHashSet<ConstraintPosition>()
            val parameterTypes = Lists.newArrayList<KotlinType>()
            for (valueParameterDescriptor in substitutedDescriptor.valueParameters) {
                parameterTypes.add(valueParameterDescriptor.type)
                if (valueParameterDescriptor.index >= inferenceErrorData.valueArgumentsTypes.size) continue
                val actualType = inferenceErrorData.valueArgumentsTypes.get(valueParameterDescriptor.index)
                if (!KotlinTypeChecker.DEFAULT.isSubtypeOf(actualType, valueParameterDescriptor.type)) {
                    errorPositions.add(VALUE_PARAMETER_POSITION.position(valueParameterDescriptor.index))
                }
            }

            if (receiverType != null && inferenceErrorData.receiverArgumentType != null
                && !KotlinTypeChecker.DEFAULT.isSubtypeOf(inferenceErrorData.receiverArgumentType, receiverType)) {
                errorPositions.add(RECEIVER_POSITION.position())
            }

            table.functionArgumentTypeList(receiverType, parameterTypes, { errorPositions.contains(it) })
        }

        table.text("can be applied to").functionArgumentTypeList(inferenceErrorData.receiverArgumentType, inferenceErrorData.valueArgumentsTypes)

        return result
    }

    @JvmStatic fun renderParameterConstraintError(
            inferenceErrorData: InferenceErrorData, renderer: TabledDescriptorRenderer
    ): TabledDescriptorRenderer {
        val constraintErrors = inferenceErrorData.constraintSystem.status.constraintErrors
        val errorPositions = constraintErrors.filter { it is ParameterConstraintError }.map { it.constraintPosition }
        return renderer.table(
                TabledDescriptorRenderer
                        .newTable()
                        .descriptor(inferenceErrorData.descriptor)
                        .text("cannot be applied to")
                        .functionArgumentTypeList(inferenceErrorData.receiverArgumentType,
                                                  inferenceErrorData.valueArgumentsTypes,
                                                  { errorPositions.contains(it) }))
    }


    @JvmStatic fun renderNoInformationForParameterError(
            inferenceErrorData: InferenceErrorData, result: TabledDescriptorRenderer
    ): TabledDescriptorRenderer {
        val firstUnknownVariable = inferenceErrorData.constraintSystem.typeVariables.firstOrNull { variable ->
            inferenceErrorData.constraintSystem.getTypeBounds(variable).values.isEmpty()
        } ?: return result.apply {
            LOG.error(debugMessage("There is no unknown parameter for 'no information for parameter error'.", inferenceErrorData))
        }

        return result
                .text(newText().normal("Not enough information to infer parameter ")
                              .strong(firstUnknownVariable.name)
                              .normal(" in "))
                .table(newTable()
                               .descriptor(inferenceErrorData.descriptor)
                               .text("Please specify it explicitly."))
    }

    @JvmStatic fun renderUpperBoundViolatedInferenceError(
            inferenceErrorData: InferenceErrorData, result: TabledDescriptorRenderer
    ): TabledDescriptorRenderer {
        val constraintSystem = inferenceErrorData.constraintSystem
        val status = constraintSystem.status
        LOG.assertTrue(status.hasViolatedUpperBound(),
                       debugMessage("Upper bound violated renderer is applied for incorrect status", inferenceErrorData))

        val systemWithoutWeakConstraints = constraintSystem.filterConstraintsOut(TYPE_BOUND_POSITION)
        val typeParameterDescriptor = inferenceErrorData.descriptor.typeParameters.firstOrNull {
            !ConstraintsUtil.checkUpperBoundIsSatisfied(systemWithoutWeakConstraints, it, inferenceErrorData.call, true)
        }

        if (typeParameterDescriptor == null) {
            if (inferenceErrorData.descriptor is TypeAliasConstructorDescriptor) {
                renderUpperBoundViolatedInferenceErrorForTypeAliasConstructor(
                        inferenceErrorData, result, systemWithoutWeakConstraints
                )?.let {
                    return it
                }
            }

            return if (status.hasConflictingConstraints())
                renderConflictingSubstitutionsInferenceError(inferenceErrorData, result)
            else {
                LOG.error(debugMessage("There is no type parameter with violated upper bound for 'upper bound violated' error", inferenceErrorData))
                result
            }
        }

        val typeVariable = systemWithoutWeakConstraints.descriptorToVariable(inferenceErrorData.call.toHandle(), typeParameterDescriptor)
        val inferredValueForTypeParameter = systemWithoutWeakConstraints.getTypeBounds(typeVariable).value
        if (inferredValueForTypeParameter == null) {
            LOG.error(debugMessage("System without weak constraints is not successful, there is no value for type parameter " +
                                   typeParameterDescriptor.name + "\n: " + systemWithoutWeakConstraints, inferenceErrorData))
            return result
        }

        result.text(newText()
                            .normal("Type parameter bound for ")
                            .strong(typeParameterDescriptor.name)
                            .normal(" in "))
                .table(newTable()
                               .descriptor(inferenceErrorData.descriptor))

        var violatedUpperBound: KotlinType? = null
        for (upperBound in typeParameterDescriptor.upperBounds) {
            val upperBoundWithSubstitutedInferredTypes = systemWithoutWeakConstraints.resultingSubstitutor.substitute(upperBound, Variance.INVARIANT)
            if (upperBoundWithSubstitutedInferredTypes != null
                && !KotlinTypeChecker.DEFAULT.isSubtypeOf(inferredValueForTypeParameter, upperBoundWithSubstitutedInferredTypes)) {
                violatedUpperBound = upperBoundWithSubstitutedInferredTypes
                break
            }
        }
        if (violatedUpperBound == null) {
            LOG.error(debugMessage("Type parameter (chosen as violating its upper bound)" +
                                   typeParameterDescriptor.name + " violates no bounds after substitution", inferenceErrorData))
            return result
        }

        // TODO: context should be in fact shared for the table and these two types
        val context = RenderingContext.of(inferredValueForTypeParameter, violatedUpperBound)
        val typeRenderer = result.typeRenderer
        result.text(newText()
                            .normal(" is not satisfied: inferred type ")
                            .error(typeRenderer.render(inferredValueForTypeParameter, context))
                            .normal(" is not a subtype of ")
                            .strong(typeRenderer.render(violatedUpperBound, context)))
        return result
    }

    private fun renderUpperBoundViolatedInferenceErrorForTypeAliasConstructor(
            inferenceErrorData: InferenceErrorData,
            result: TabledDescriptorRenderer,
            systemWithoutWeakConstraints: ConstraintSystem
    ): TabledDescriptorRenderer? {
        val descriptor = inferenceErrorData.descriptor
        if (descriptor !is TypeAliasConstructorDescriptor) {
            LOG.error("Type alias constructor descriptor expected: $descriptor")
            return result
        }

        val inferredTypesForTypeParameters = descriptor.typeParameters.map {
            val typeVariable = systemWithoutWeakConstraints.descriptorToVariable(inferenceErrorData.call.toHandle(), it)
            systemWithoutWeakConstraints.getTypeBounds(typeVariable).value
        }
        val inferredTypeSubstitutor = TypeSubstitutor.create(object : TypeConstructorSubstitution() {
            override fun get(key: TypeConstructor): TypeProjection? {
                val typeDescriptor = key.declarationDescriptor as? TypeParameterDescriptor ?: return null
                if (typeDescriptor.containingDeclaration != descriptor.typeAliasDescriptor) return null
                return inferredTypesForTypeParameters[typeDescriptor.index]?.let(::TypeProjectionImpl)
            }
        })

        for (constraintError in inferenceErrorData.constraintSystem.status.constraintErrors) {
            val constraintInfo = constraintError.constraintPosition.getValidityConstraintForConstituentType() ?: continue

            val violatedUpperBound = inferredTypeSubstitutor.safeSubstitute(constraintInfo.bound, Variance.INVARIANT)
            val violatingInferredType = inferredTypeSubstitutor.safeSubstitute(constraintInfo.typeArgument, Variance.INVARIANT)

            val context = RenderingContext.of(violatingInferredType, violatedUpperBound)
            val typeRenderer = result.typeRenderer

            result.text(newText().normal("Type parameter bound for ").strong(constraintInfo.typeParameter.name)
                                .normal(" in type inferred from type alias expansion for "))
                    .table(newTable().descriptor(inferenceErrorData.descriptor))

            result.text(newText().normal(" is not satisfied: inferred type ").error(typeRenderer.render(violatingInferredType, context))
                                .normal(" is not a subtype of ").strong(typeRenderer.render(violatedUpperBound, context)))

            return result
        }

        return null
    }

    @JvmStatic fun renderCannotCaptureTypeParameterError(
            inferenceErrorData: InferenceErrorData, result: TabledDescriptorRenderer
    ): TabledDescriptorRenderer {
        val system = inferenceErrorData.constraintSystem
        val errors = system.status.constraintErrors
        val typeVariableWithCapturedConstraint = errors.firstIsInstanceOrNull<CannotCapture>()?.typeVariable
        if (typeVariableWithCapturedConstraint == null) {
            LOG.error(debugMessage("An error 'cannot capture type parameter' is not found in errors", inferenceErrorData))
            return result
        }

        val typeBounds = system.getTypeBounds(typeVariableWithCapturedConstraint)
        val boundWithCapturedType = typeBounds.bounds.firstOrNull { it.constrainingType.isCaptured() }
        val capturedTypeConstructor = boundWithCapturedType?.constrainingType?.constructor as? CapturedTypeConstructor
        if (capturedTypeConstructor == null) {
            LOG.error(debugMessage("There is no captured type in bounds, but there is an error 'cannot capture type parameter'", inferenceErrorData))
            return result
        }

        val typeParameter = typeVariableWithCapturedConstraint.originalTypeParameter
        val upperBound = TypeIntersector.getUpperBoundsAsType(typeParameter)

        assert(!KotlinBuiltIns.isNullableAny(upperBound) && capturedTypeConstructor.typeProjection.projectionKind == Variance.IN_VARIANCE) {
            "There is the only reason to report TYPE_INFERENCE_CANNOT_CAPTURE_TYPES"
        }

        val explanation =
                "Type parameter has an upper bound '" + result.typeRenderer.render(upperBound, RenderingContext.of(upperBound)) + "'" +
                " that cannot be satisfied capturing 'in' projection"

        result.text(newText().normal(
                "'" + typeParameter.name + "'" +
                " cannot capture " +
                "'" + capturedTypeConstructor.typeProjection + "'. " +
                explanation
        ))
        return result
    }

    @JvmField val CLASSES_OR_SEPARATED = Renderer<Collection<ClassDescriptor>> {
        descriptors ->
        buildString {
            var index = 0
            for (descriptor in descriptors) {
                append(DescriptorUtils.getFqName(descriptor).asString())
                index++
                if (index <= descriptors.size - 2) {
                    append(", ")
                }
                else if (index == descriptors.size - 1) {
                    append(" or ")
                }
            }
        }
    }

    private fun renderTypes(types: Collection<KotlinType>, context: RenderingContext) = StringUtil.join(types, { RENDER_TYPE.render(it, context) }, ", ")

    @JvmField val RENDER_COLLECTION_OF_TYPES = ContextDependentRenderer<Collection<KotlinType>> { types, context -> renderTypes(types, context) }

    fun renderConstraintSystem(constraintSystem: ConstraintSystem, shortTypeBounds: Boolean): String {
        val typeBounds = linkedSetOf<TypeBounds>()
        for (variable in constraintSystem.typeVariables) {
            typeBounds.add(constraintSystem.getTypeBounds(variable))
        }
        return "type parameter bounds:\n" +
               StringUtil.join(typeBounds, { renderTypeBounds(it, short = shortTypeBounds) }, "\n") + "\n\n" + "status:\n" +
               ConstraintsUtil.getDebugMessageForStatus(constraintSystem.status)
    }

    private fun renderTypeBounds(typeBounds: TypeBounds, short: Boolean): String {
        val renderBound = { bound: Bound ->
            val arrow = when (bound.kind) {
                LOWER_BOUND -> ">: "
                UPPER_BOUND -> "<: "
                else -> ":= "
            }
            val renderer = if (short) DescriptorRenderer.SHORT_NAMES_IN_TYPES else DescriptorRenderer.FQ_NAMES_IN_TYPES
            val renderedBound = arrow + renderer.renderType(bound.constrainingType) +  if (!bound.isProper) "*" else ""
            if (short) renderedBound else renderedBound + '(' + bound.position + ')'
        }
        val typeVariableName = typeBounds.typeVariable.name
        return if (typeBounds.bounds.isEmpty()) {
            typeVariableName.asString()
        }
        else
            "$typeVariableName ${StringUtil.join(typeBounds.bounds, renderBound, ", ")}"
    }

    private fun debugMessage(message: String, inferenceErrorData: InferenceErrorData) = buildString {
        append(message)
        append("\nConstraint system: \n")
        append(renderConstraintSystem(inferenceErrorData.constraintSystem, false))
        append("\nDescriptor:\n")
        append(inferenceErrorData.descriptor)
        append("\nExpected type:\n")
        val context = RenderingContext.Empty
        if (TypeUtils.noExpectedType(inferenceErrorData.expectedType)) {
            append(inferenceErrorData.expectedType)
        }
        else {
            append(RENDER_TYPE.render(inferenceErrorData.expectedType, context))
        }
        append("\nArgument types:\n")
        if (inferenceErrorData.receiverArgumentType != null) {
            append(RENDER_TYPE.render(inferenceErrorData.receiverArgumentType, context)).append(".")
        }
        append("(").append(renderTypes(inferenceErrorData.valueArgumentsTypes, context)).append(")")
    }

    private val WHEN_MISSING_LIMIT = 7

    @JvmField val RENDER_WHEN_MISSING_CASES = Renderer<List<WhenMissingCase>> {
        if (!it.hasUnknown) {
            val list = it.joinToString(", ", limit = WHEN_MISSING_LIMIT) { "'$it'" }
            val branches = if (it.size > 1) "branches" else "branch"
            "$list $branches or 'else' branch instead"
        }
        else {
            "'else' branch"
        }
    }

    @JvmField val FQ_NAMES_IN_TYPES = DescriptorRenderer.FQ_NAMES_IN_TYPES.asRenderer()
    @JvmField val COMPACT = DescriptorRenderer.COMPACT.asRenderer()
    @JvmField val WITHOUT_MODIFIERS = DescriptorRenderer.withOptions {
        modifiers = emptySet()
    }.asRenderer()
    @JvmField val SHORT_NAMES_IN_TYPES = DescriptorRenderer.SHORT_NAMES_IN_TYPES.asRenderer()
    @JvmField val COMPACT_WITH_MODIFIERS = DescriptorRenderer.COMPACT_WITH_MODIFIERS.asRenderer()
    @JvmField val DEPRECATION_RENDERER = DescriptorRenderer.ONLY_NAMES_WITH_SHORT_TYPES.withOptions {
        withoutTypeParameters = false
        receiverAfterName = false
        renderAccessors = true
    }.asRenderer()
}

fun DescriptorRenderer.asRenderer() = SmartDescriptorRenderer(this)
