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

package org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder

import org.jetbrains.kotlin.types.JetType
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.scopes.JetScope
import org.jetbrains.kotlin.resolve.BindingContext
import com.intellij.refactoring.psi.SearchUtils
import org.jetbrains.kotlin.idea.references.JetSimpleNameReference
import org.jetbrains.kotlin.types.ErrorUtils
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.checker.JetTypeChecker
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.TypeProjectionImpl
import org.jetbrains.kotlin.types.JetTypeImpl
import org.jetbrains.kotlin.psi.psiUtil.getAssignmentByLHS
import org.jetbrains.kotlin.resolve.bindingContextUtil.isUsedAsStatement
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.idea.util.makeNotNullable
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.resolve.descriptorUtil.resolveTopLevelClass
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.calls.callUtil.*
import org.jetbrains.kotlin.resolve.calls.smartcasts.*
import java.util.*

private fun JetType.contains(inner: JetType): Boolean {
    return JetTypeChecker.DEFAULT.equalTypes(this, inner) || getArguments().any { inner in it.getType() }
}

private fun DeclarationDescriptor.render(
        typeParameterNameMap: Map<TypeParameterDescriptor, String>,
        fq: Boolean
): String = when {
    this is TypeParameterDescriptor -> typeParameterNameMap[this] ?: getName().asString()
    fq -> DescriptorUtils.getFqName(this).asString()
    else -> getName().asString()
}

private fun JetType.render(typeParameterNameMap: Map<TypeParameterDescriptor, String>, fq: Boolean): String {
    val arguments = getArguments().map { it.getType().render(typeParameterNameMap, fq) }
    val typeString = getConstructor().getDeclarationDescriptor()!!.render(typeParameterNameMap, fq)
    val typeArgumentString = if (arguments.notEmpty) arguments.joinToString(", ", "<", ">") else ""
    val nullifier = if (isMarkedNullable()) "?" else ""
    return "$typeString$typeArgumentString$nullifier"
}

private fun JetType.renderShort(typeParameterNameMap: Map<TypeParameterDescriptor, String>) = render(typeParameterNameMap, false)
private fun JetType.renderLong(typeParameterNameMap: Map<TypeParameterDescriptor, String>) = render(typeParameterNameMap, true)

private fun getTypeParameterNamesNotInScope(typeParameters: Collection<TypeParameterDescriptor>, scope: JetScope): List<TypeParameterDescriptor> {
    return typeParameters.filter { typeParameter ->
        val classifier = scope.getClassifier(typeParameter.getName())
        classifier == null || classifier != typeParameter
    }
}

fun JetType.getTypeParameters(): Set<TypeParameterDescriptor> {
    val typeParameters = LinkedHashSet<TypeParameterDescriptor>()
    val arguments = getArguments()
    if (arguments.empty) {
        val descriptor = getConstructor().getDeclarationDescriptor()
        if (descriptor is TypeParameterDescriptor) {
            typeParameters.add(descriptor as TypeParameterDescriptor)
        }
    }
    else {
        arguments.flatMapTo(typeParameters) { projection ->
            projection.getType().getTypeParameters()
        }
    }
    return typeParameters
}

fun JetExpression.guessTypes(
        context: BindingContext,
        module: ModuleDescriptor?,
        coerceUnusedToUnit: Boolean = true
): Array<JetType> {
    val builtIns = KotlinBuiltIns.getInstance()

    if (coerceUnusedToUnit
        && this !is JetDeclaration
        && isUsedAsStatement(context)
        && getNonStrictParentOfType<JetAnnotationEntry>() == null) return array(builtIns.getUnitType())

    // if we know the actual type of the expression
    val theType1 = context[BindingContext.EXPRESSION_TYPE, this]
    if (theType1 != null) {
        val dataFlowInfo = context[BindingContext.EXPRESSION_DATA_FLOW_INFO, this]
        val possibleTypes = dataFlowInfo?.getPossibleTypes(DataFlowValueFactory.createDataFlowValue(this, theType1, context))
        return if (possibleTypes != null && possibleTypes.isNotEmpty()) possibleTypes.copyToArray() else array(theType1)
    }

    // expression has an expected type
    val theType2 = context[BindingContext.EXPECTED_EXPRESSION_TYPE, this]
    if (theType2 != null) {
        return array(theType2)
    }

    val parent = getParent()
    return when {
        this is JetTypeConstraint -> {
            // expression itself is a type assertion
            val constraint = (this as JetTypeConstraint)
            array(context[BindingContext.TYPE, constraint.getBoundTypeReference()]!!)
        }
        parent is JetTypeConstraint -> {
            // expression is on the left side of a type assertion
            val constraint = (parent as JetTypeConstraint)
            array(context[BindingContext.TYPE, constraint.getBoundTypeReference()]!!)
        }
        this is JetMultiDeclarationEntry -> {
            // expression is on the lhs of a multi-declaration
            val typeRef = getTypeReference()
            if (typeRef != null) {
                // and has a specified type
                array(context[BindingContext.TYPE, typeRef]!!)
            }
            else {
                // otherwise guess
                guessType(context)
            }
        }
        this is JetParameter -> {
            // expression is a parameter (e.g. declared in a for-loop)
            val typeRef = getTypeReference()
            if (typeRef != null) {
                // and has a specified type
                array(context[BindingContext.TYPE, typeRef]!!)
            }
            else {
                // otherwise guess
                guessType(context)
            }
        }
        parent is JetVariableDeclaration -> {
            // the expression is the RHS of a variable assignment with a specified type
            val variable = parent as JetVariableDeclaration
            val typeRef = variable.getTypeReference()
            if (typeRef != null) {
                // and has a specified type
                array(context[BindingContext.TYPE, typeRef]!!)
            }
            else {
                // otherwise guess, based on LHS
                variable.guessType(context)
            }
        }
        parent is JetPropertyDelegate && module != null -> {
            val property = context[BindingContext.DECLARATION_TO_DESCRIPTOR, parent.getParent() as JetProperty] as PropertyDescriptor
            val delegateClassName = if (property.isVar()) "ReadWriteProperty" else "ReadOnlyProperty"
            val delegateClass = module.resolveTopLevelClass(FqName("kotlin.properties.$delegateClassName"))
                                ?: return array(builtIns.getAnyType())
            val receiverType = (property.getExtensionReceiverParameter() ?: property.getDispatchReceiverParameter())?.getType()
                               ?: builtIns.getNullableNothingType()
            val typeArguments = listOf(TypeProjectionImpl(receiverType), TypeProjectionImpl(property.getType()))
            array(TypeUtils.substituteProjectionsForParameters(delegateClass, typeArguments))
        }
        parent is JetStringTemplateEntryWithExpression && parent.getExpression() == this -> {
            array(KotlinBuiltIns.getInstance().getStringType())
        }
        else -> array() // can't infer anything
    }
}

private fun JetNamedDeclaration.guessType(context: BindingContext): Array<JetType> {
    val expectedTypes = SearchUtils.findAllReferences(this, getUseScope())!!.stream().map { ref ->
        if (ref is JetSimpleNameReference) {
            context[BindingContext.EXPECTED_EXPRESSION_TYPE, ref.expression]
        }
        else {
            null
        }
    }.filterNotNullTo(HashSet<JetType>())

    if (expectedTypes.isEmpty() || expectedTypes.any { expectedType -> ErrorUtils.containsErrorType(expectedType) }) {
        return array<JetType>()
    }
    val theType = TypeUtils.intersect(JetTypeChecker.DEFAULT, expectedTypes)
    if (theType != null) {
        return array<JetType>(theType)
    }
    else {
        // intersection doesn't exist; let user make an imperfect choice
        return expectedTypes.copyToArray()
    }
}

/**
 * Encapsulates a single type substitution of a <code>JetType</code> by another <code>JetType</code>.
 */
private class JetTypeSubstitution(public val forType: JetType, public val byType: JetType)

private fun JetType.substitute(substitution: JetTypeSubstitution, variance: Variance): JetType {
    val nullable = isMarkedNullable()
    val currentType = makeNotNullable()

    if (when (variance) {
        Variance.INVARIANT      -> JetTypeChecker.DEFAULT.equalTypes(currentType, substitution.forType)
        Variance.IN_VARIANCE    -> JetTypeChecker.DEFAULT.isSubtypeOf(currentType, substitution.forType)
        Variance.OUT_VARIANCE   -> JetTypeChecker.DEFAULT.isSubtypeOf(substitution.forType, currentType)
    }) {
        return TypeUtils.makeNullableAsSpecified(substitution.byType, nullable)
    }
    else {
        val newArguments = getArguments().zip(getConstructor().getParameters()).map { pair ->
            val (projection, typeParameter) = pair
            TypeProjectionImpl(Variance.INVARIANT, projection.getType().substitute(substitution, typeParameter.getVariance()))
        }
        return JetTypeImpl(getAnnotations(), getConstructor(), isMarkedNullable(), newArguments, getMemberScope())
    }
}

fun JetExpression.getExpressionForTypeGuess() = getAssignmentByLHS()?.getRight() ?: this

fun JetCallElement.getTypeInfoForTypeArguments(): List<TypeInfo> {
    return getTypeArguments().map { it.getTypeReference()?.let { TypeInfo(it, Variance.INVARIANT) } }.filterNotNull()
}