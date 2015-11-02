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

package org.jetbrains.kotlin.diagnostics.rendering

import com.google.common.collect.Lists
import com.google.common.collect.Sets
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.diagnostics.rendering.TabledDescriptorRenderer.newTable
import org.jetbrains.kotlin.diagnostics.rendering.TabledDescriptorRenderer.newText
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.renderer.Renderer
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.MemberComparator
import org.jetbrains.kotlin.resolve.calls.inference.*
import org.jetbrains.kotlin.resolve.calls.inference.TypeBounds.Bound
import org.jetbrains.kotlin.resolve.calls.inference.TypeBounds.BoundKind.LOWER_BOUND
import org.jetbrains.kotlin.resolve.calls.inference.TypeBounds.BoundKind.UPPER_BOUND
import org.jetbrains.kotlin.resolve.calls.inference.constraintPosition.ConstraintPosition
import org.jetbrains.kotlin.resolve.calls.inference.constraintPosition.ConstraintPositionKind
import org.jetbrains.kotlin.resolve.calls.inference.constraintPosition.ConstraintPositionKind.RECEIVER_POSITION
import org.jetbrains.kotlin.resolve.calls.inference.constraintPosition.ConstraintPositionKind.VALUE_PARAMETER_POSITION
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeIntersector
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker
import java.io.PrintWriter
import java.io.StringWriter

public object Renderers {

    private val LOG = Logger.getInstance(javaClass<Renderers>())

    public val TO_STRING: Renderer<Any> = Renderer {
        element ->
        if (element is DeclarationDescriptor) {
            LOG.warn("Diagnostic renderer TO_STRING was used to render an instance of DeclarationDescriptor.\n"
                     + "This is usually a bad idea, because descriptors' toString() includes some debug information, "
                     + "which should not be seen by the user.\nDescriptor: " + element)
        }
        element.toString()
    }

    public val STRING: Renderer<String> = Renderer { it }

    public val THROWABLE: Renderer<Throwable> = Renderer {
        val writer = StringWriter()
        it.printStackTrace(PrintWriter(writer))
        StringUtil.first(writer.toString(), 2048, true)
    }

    public val NAME: Renderer<Named> = Renderer { it.getName().asString() }

    public val NAME_OF_PARENT_OR_FILE: Renderer<DeclarationDescriptor> = Renderer {
        if (DescriptorUtils.isTopLevelDeclaration(it) && it is DeclarationDescriptorWithVisibility && it.visibility == Visibilities.PRIVATE) {
            "file"
        }
        else {
            "'" + it.containingDeclaration!!.name + "'"
        }
    }

    public val ELEMENT_TEXT: Renderer<PsiElement> = Renderer { it.getText() }

    public val DECLARATION_NAME: Renderer<KtNamedDeclaration> = Renderer { it.getNameAsSafeName().asString() }

    public val RENDER_CLASS_OR_OBJECT: Renderer<KtClassOrObject> = Renderer {
        classOrObject: KtClassOrObject ->
        val name = if (classOrObject.getName() != null) " '" + classOrObject.getName() + "'" else ""
        if (classOrObject is KtClass) "Class" + name else "Object" + name
    }

    public val RENDER_CLASS_OR_OBJECT_NAME: Renderer<ClassDescriptor> = Renderer { it.renderKindWithName() }

    public val RENDER_TYPE: Renderer<KotlinType> = Renderer { DescriptorRenderer.FQ_NAMES_IN_TYPES.renderType(it) }

    public val RENDER_POSITION_VARIANCE: Renderer<Variance> = Renderer {
        variance: Variance ->
        when (variance) {
            Variance.INVARIANT -> "invariant"
            Variance.IN_VARIANCE -> "in"
            Variance.OUT_VARIANCE -> "out"
        }
    }

    public val AMBIGUOUS_CALLS: Renderer<Collection<ResolvedCall<*>>> = Renderer {
        calls: Collection<ResolvedCall<*>> ->
        calls
                .map { it.getResultingDescriptor() }
                .sortedWith(MemberComparator.INSTANCE)
                .joinToString(separator = "\n", prefix = "\n") { DescriptorRenderer.FQ_NAMES_IN_TYPES.render(it) }
    }

    @JvmStatic
    public fun <T> commaSeparated(itemRenderer: Renderer<T>): Renderer<Collection<T>> = Renderer {
        collection ->
        StringBuilder {
            val iterator = collection.iterator()
            while (iterator.hasNext()) {
                val next = iterator.next()
                append(itemRenderer.render(next))
                if (iterator.hasNext()) {
                    append(", ")
                }
            }
        }.toString()
    }

    public val TYPE_INFERENCE_CONFLICTING_SUBSTITUTIONS_RENDERER: Renderer<InferenceErrorData> = Renderer {
        renderConflictingSubstitutionsInferenceError(it, TabledDescriptorRenderer.create()).toString()
    }

    public val TYPE_INFERENCE_PARAMETER_CONSTRAINT_ERROR_RENDERER: Renderer<InferenceErrorData> = Renderer {
        renderParameterConstraintError(it, TabledDescriptorRenderer.create()).toString()
    }

    public val TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER_RENDERER: Renderer<InferenceErrorData> = Renderer {
        renderNoInformationForParameterError(it, TabledDescriptorRenderer.create()).toString()
    }

    public val TYPE_INFERENCE_UPPER_BOUND_VIOLATED_RENDERER: Renderer<InferenceErrorData> = Renderer {
        renderUpperBoundViolatedInferenceError(it, TabledDescriptorRenderer.create()).toString()
    }

    public val TYPE_INFERENCE_CANNOT_CAPTURE_TYPES_RENDERER: Renderer<InferenceErrorData> = Renderer {
        renderCannotCaptureTypeParameterError(it, TabledDescriptorRenderer.create()).toString()
    }

    @JvmStatic
    public fun renderConflictingSubstitutionsInferenceError(
            inferenceErrorData: InferenceErrorData, result: TabledDescriptorRenderer
    ): TabledDescriptorRenderer {
        LOG.assertTrue(inferenceErrorData.constraintSystem.status.hasConflictingConstraints(),
                       renderDebugMessage("Conflicting substitutions inference error renderer is applied for incorrect status", inferenceErrorData))

        val substitutedDescriptors = Lists.newArrayList<CallableDescriptor>()
        val substitutors = ConstraintsUtil.getSubstitutorsForConflictingParameters(inferenceErrorData.constraintSystem)
        for (substitutor in substitutors) {
            val substitutedDescriptor = inferenceErrorData.descriptor.substitute(substitutor)
            substitutedDescriptors.add(substitutedDescriptor)
        }

        val firstConflictingParameter = ConstraintsUtil.getFirstConflictingParameter(inferenceErrorData.constraintSystem)
        if (firstConflictingParameter == null) {
            LOG.error(renderDebugMessage("There is no conflicting parameter for 'conflicting constraints' error.", inferenceErrorData))
            return result
        }

        result.text(newText()
                            .normal("Cannot infer type parameter ")
                            .strong(firstConflictingParameter.getName())
                            .normal(" in "))
        val table = newTable()
        result.table(table)
        table.descriptor(inferenceErrorData.descriptor).text("None of the following substitutions")

        for (substitutedDescriptor in substitutedDescriptors) {
            val receiverType = DescriptorUtils.getReceiverParameterType(substitutedDescriptor.getExtensionReceiverParameter())

            val errorPositions = Sets.newHashSet<ConstraintPosition>()
            val parameterTypes = Lists.newArrayList<KotlinType>()
            for (valueParameterDescriptor in substitutedDescriptor.getValueParameters()) {
                parameterTypes.add(valueParameterDescriptor.getType())
                if (valueParameterDescriptor.index >= inferenceErrorData.valueArgumentsTypes.size()) continue
                val actualType = inferenceErrorData.valueArgumentsTypes.get(valueParameterDescriptor.index)
                if (!KotlinTypeChecker.DEFAULT.isSubtypeOf(actualType, valueParameterDescriptor.getType())) {
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

    @JvmStatic
    public fun renderParameterConstraintError(
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


    @JvmStatic
    public fun renderNoInformationForParameterError(
            inferenceErrorData: InferenceErrorData, result: TabledDescriptorRenderer
    ): TabledDescriptorRenderer {
        var firstUnknownParameter: TypeParameterDescriptor? = null
        for (typeParameter in inferenceErrorData.constraintSystem.typeParameterDescriptors) {
            if (inferenceErrorData.constraintSystem.getTypeBounds(typeParameter).values.isEmpty()) {
                firstUnknownParameter = typeParameter
                break
            }
        }
        if (firstUnknownParameter == null) {
            LOG.error(renderDebugMessage("There is no unknown parameter for 'no information for parameter error'.", inferenceErrorData))
            return result
        }

        return result
                .text(newText().normal("Not enough information to infer parameter ")
                              .strong(firstUnknownParameter.getName())
                              .normal(" in "))
                .table(newTable()
                               .descriptor(inferenceErrorData.descriptor)
                               .text("Please specify it explicitly."))
    }

    @JvmStatic
    public fun renderUpperBoundViolatedInferenceError(
            inferenceErrorData: InferenceErrorData, result: TabledDescriptorRenderer
    ): TabledDescriptorRenderer {
        val constraintSystem = inferenceErrorData.constraintSystem
        val status = constraintSystem.status
        LOG.assertTrue(status.hasViolatedUpperBound(),
                       renderDebugMessage("Upper bound violated renderer is applied for incorrect status", inferenceErrorData))

        val systemWithoutWeakConstraints = constraintSystem.filterConstraintsOut(ConstraintPositionKind.TYPE_BOUND_POSITION)
        val typeParameterDescriptor = inferenceErrorData.descriptor.typeParameters.firstOrNull {
            !ConstraintsUtil.checkUpperBoundIsSatisfied(systemWithoutWeakConstraints, it, true)
        }
        if (typeParameterDescriptor == null && status.hasConflictingConstraints()) {
            return renderConflictingSubstitutionsInferenceError(inferenceErrorData, result)
        }
        if (typeParameterDescriptor == null) {
            LOG.error(renderDebugMessage("There is no type parameter with violated upper bound for 'upper bound violated' error", inferenceErrorData))
            return result
        }

        val inferredValueForTypeParameter = systemWithoutWeakConstraints.getTypeBounds(typeParameterDescriptor).value
        if (inferredValueForTypeParameter == null) {
            LOG.error(renderDebugMessage("System without weak constraints is not successful, there is no value for type parameter " + 
                                         typeParameterDescriptor.getName() + "\n: " + systemWithoutWeakConstraints, inferenceErrorData))
            return result
        }

        result.text(newText()
                            .normal("Type parameter bound for ")
                            .strong(typeParameterDescriptor.getName())
                            .normal(" in "))
                .table(newTable()
                               .descriptor(inferenceErrorData.descriptor))

        var violatedUpperBound: KotlinType? = null
        for (upperBound in typeParameterDescriptor.getUpperBounds()) {
            val upperBoundWithSubstitutedInferredTypes = systemWithoutWeakConstraints.resultingSubstitutor.substitute(upperBound, Variance.INVARIANT)
            if (upperBoundWithSubstitutedInferredTypes != null
                && !KotlinTypeChecker.DEFAULT.isSubtypeOf(inferredValueForTypeParameter, upperBoundWithSubstitutedInferredTypes)) {
                violatedUpperBound = upperBoundWithSubstitutedInferredTypes
                break
            }
        }
        if (violatedUpperBound == null) {
            LOG.error(renderDebugMessage("Type parameter (chosen as violating its upper bound)" + 
                                         typeParameterDescriptor.getName() + " violates no bounds after substitution", inferenceErrorData))
            return result
        }

        val typeRenderer = result.getTypeRenderer()
        result.text(newText()
                            .normal(" is not satisfied: inferred type ")
                            .error(typeRenderer.render(inferredValueForTypeParameter))
                            .normal(" is not a subtype of ")
                            .strong(typeRenderer.render(violatedUpperBound)))
        return result
    }

    @JvmStatic
    public fun renderCannotCaptureTypeParameterError(
            inferenceErrorData: InferenceErrorData, result: TabledDescriptorRenderer
    ): TabledDescriptorRenderer {
        val constraintSystem = inferenceErrorData.constraintSystem
        val errors = constraintSystem.status.constraintErrors
        val typeParameterWithCapturedConstraint = (errors.firstOrNull { it is CannotCapture } as? CannotCapture)?.typeVariable
        if (typeParameterWithCapturedConstraint == null) {
            LOG.error(renderDebugMessage("An error 'cannot capture type parameter' is not found in errors", inferenceErrorData))
            return result
        }

        val typeBounds = constraintSystem.getTypeBounds(typeParameterWithCapturedConstraint)
        val boundWithCapturedType = typeBounds.bounds.firstOrNull { it.constrainingType.isCaptured() }
        val capturedTypeConstructor = boundWithCapturedType?.constrainingType?.getConstructor() as? CapturedTypeConstructor
        if (capturedTypeConstructor == null) {
            LOG.error(renderDebugMessage("There is no captured type in bounds, but there is an error 'cannot capture type parameter'", inferenceErrorData))
            return result
        }

        val explanation: String
        val upperBound = TypeIntersector.getUpperBoundsAsType(typeParameterWithCapturedConstraint)
        if (!KotlinBuiltIns.isNullableAny(upperBound) && capturedTypeConstructor.typeProjection.getProjectionKind() == Variance.IN_VARIANCE) {
            explanation = "Type parameter has an upper bound '" + result.getTypeRenderer().render(upperBound) + "'" +
                          " that cannot be satisfied capturing 'in' projection"
        }
        else {
            explanation = "Only top-level type projections can be captured"
        }
        result.text(newText().normal("'" + typeParameterWithCapturedConstraint.getName() + "'" +
                                     " cannot capture " +
                                     "'" + capturedTypeConstructor.typeProjection + "'. " +
                                     explanation))
        return result
    }

    public val CLASSES_OR_SEPARATED: Renderer<Collection<ClassDescriptor>> = Renderer {
        descriptors ->
        StringBuilder {
            var index = 0
            for (descriptor in descriptors) {
                append(DescriptorUtils.getFqName(descriptor).asString())
                index++
                if (index <= descriptors.size() - 2) {
                    append(", ")
                }
                else if (index == descriptors.size() - 1) {
                    append(" or ")
                }
            }
        }.toString()
    }

    private fun renderTypes(types: Collection<KotlinType>) = StringUtil.join(types, { RENDER_TYPE.render(it) }, ", ")

    public val RENDER_COLLECTION_OF_TYPES: Renderer<Collection<KotlinType>> = Renderer { renderTypes(it) }

    private fun renderConstraintSystem(constraintSystem: ConstraintSystem, renderTypeBounds: Renderer<TypeBounds>): String {
        val typeVariables = constraintSystem.typeParameterDescriptors
        val typeBounds = linkedSetOf<TypeBounds>()
        for (variable in typeVariables) {
            typeBounds.add(constraintSystem.getTypeBounds(variable))
        }
        return "type parameter bounds:\n" +
               StringUtil.join(typeBounds, { renderTypeBounds.render(it) }, "\n") + "\n\n" + "status:\n" +
               ConstraintsUtil.getDebugMessageForStatus(constraintSystem.status)
    }

    public val RENDER_CONSTRAINT_SYSTEM: Renderer<ConstraintSystem> = Renderer { renderConstraintSystem(it, RENDER_TYPE_BOUNDS) }
    public val RENDER_CONSTRAINT_SYSTEM_SHORT: Renderer<ConstraintSystem> = Renderer { renderConstraintSystem(it, RENDER_TYPE_BOUNDS_SHORT) }

    private fun renderTypeBounds(typeBounds: TypeBounds, short: Boolean): String {
        val renderBound = { bound: Bound ->
            val arrow = if (bound.kind == LOWER_BOUND) ">: " else if (bound.kind == UPPER_BOUND) "<: " else ":= "
            val renderer = if (short) DescriptorRenderer.SHORT_NAMES_IN_TYPES else DescriptorRenderer.FQ_NAMES_IN_TYPES
            val renderedBound = arrow + renderer.renderType(bound.constrainingType) +  if (!bound.isProper) "*" else ""
            if (short) renderedBound else renderedBound + '(' + bound.position + ')'
        }
        val typeVariableName = typeBounds.typeVariable.getName()
        return if (typeBounds.bounds.isEmpty()) {
            typeVariableName.asString()
        }
        else
            "$typeVariableName ${StringUtil.join(typeBounds.bounds, renderBound, ", ")}"
    }

    public val RENDER_TYPE_BOUNDS: Renderer<TypeBounds> = Renderer { renderTypeBounds(it, short = false) }
    public val RENDER_TYPE_BOUNDS_SHORT: Renderer<TypeBounds> = Renderer { renderTypeBounds(it, short = true) }

    private fun renderDebugMessage(message: String, inferenceErrorData: InferenceErrorData) = StringBuilder {
        append(message)
        append("\nConstraint system: \n")
        append(RENDER_CONSTRAINT_SYSTEM.render(inferenceErrorData.constraintSystem))
        append("\nDescriptor:\n")
        append(inferenceErrorData.descriptor)
        append("\nExpected type:\n")
        if (TypeUtils.noExpectedType(inferenceErrorData.expectedType)) {
            append(inferenceErrorData.expectedType)
        }
        else {
            append(RENDER_TYPE.render(inferenceErrorData.expectedType))
        }
        append("\nArgument types:\n")
        if (inferenceErrorData.receiverArgumentType != null) {
            append(RENDER_TYPE.render(inferenceErrorData.receiverArgumentType)).append(".")
        }
        append("(").append(renderTypes(inferenceErrorData.valueArgumentsTypes)).append(")")
    }.toString()
}
