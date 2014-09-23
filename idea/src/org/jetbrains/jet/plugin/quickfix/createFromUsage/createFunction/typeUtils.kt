package org.jetbrains.jet.plugin.quickfix.createFromUsage.createFunction

import org.jetbrains.jet.lang.types.JetType
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor
import org.jetbrains.jet.lang.resolve.DescriptorUtils
import org.jetbrains.jet.lang.resolve.scopes.JetScope
import java.util.LinkedHashSet
import org.jetbrains.jet.lang.psi.JetExpression
import org.jetbrains.jet.lang.resolve.BindingContext
import org.jetbrains.jet.lang.psi.JetTypeConstraint
import org.jetbrains.jet.lang.psi.JetMultiDeclarationEntry
import org.jetbrains.jet.lang.psi.JetParameter
import org.jetbrains.jet.lang.psi.JetVariableDeclaration
import org.jetbrains.jet.lang.psi.JetNamedDeclaration
import com.intellij.refactoring.psi.SearchUtils
import org.jetbrains.jet.plugin.references.JetSimpleNameReference
import java.util.HashSet
import org.jetbrains.jet.lang.types.ErrorUtils
import org.jetbrains.jet.lang.types.TypeUtils
import org.jetbrains.jet.lang.types.checker.JetTypeChecker
import org.jetbrains.jet.lang.types.Variance
import org.jetbrains.jet.lang.types.TypeProjectionImpl
import org.jetbrains.jet.lang.types.JetTypeImpl

private fun JetType.contains(inner: JetType): Boolean {
    return this == inner || getArguments().any { inner in it.getType() }
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
    return "$typeString$typeArgumentString"
}

private fun JetType.renderShort(typeParameterNameMap: Map<TypeParameterDescriptor, String>) = render(typeParameterNameMap, false)
private fun JetType.renderLong(typeParameterNameMap: Map<TypeParameterDescriptor, String>) = render(typeParameterNameMap, true)

private fun getTypeParameterNamesNotInScope(typeParameters: Collection<TypeParameterDescriptor>, scope: JetScope): List<TypeParameterDescriptor> {
    return typeParameters.filter { typeParameter ->
        val classifier = scope.getClassifier(typeParameter.getName())
        classifier == null || classifier != typeParameter
    }
}

private fun JetType.getTypeParameters(): Set<TypeParameterDescriptor> {
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

private fun JetExpression.guessTypes(context: BindingContext): Array<JetType> {
    // if we know the actual type of the expression
    val theType1 = context[BindingContext.EXPRESSION_TYPE, this]
    if (theType1 != null) {
        return array(theType1)
    }

    // expression has an expected type
    val theType2 = context[BindingContext.EXPECTED_EXPRESSION_TYPE, this]
    if (theType2 != null) {
        return array(theType2)
    }

    return when {
        this is JetTypeConstraint -> {
            // expression itself is a type assertion
            val constraint = (this as JetTypeConstraint)
            array(context[BindingContext.TYPE, constraint.getBoundTypeReference()]!!)
        }
        getParent() is JetTypeConstraint -> {
            // expression is on the left side of a type assertion
            val constraint = (getParent() as JetTypeConstraint)
            array(context[BindingContext.TYPE, constraint.getBoundTypeReference()]!!)
        }
        this is JetMultiDeclarationEntry -> {
            // expression is on the lhs of a multi-declaration
            val typeRef = getTypeRef()
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
        getParent() is JetVariableDeclaration -> {
            // the expression is the RHS of a variable assignment with a specified type
            val variable = getParent() as JetVariableDeclaration
            val typeRef = variable.getTypeRef()
            if (typeRef != null) {
                // and has a specified type
                array(context[BindingContext.TYPE, typeRef]!!)
            }
            else {
                // otherwise guess, based on LHS
                variable.guessType(context)
            }
        }
        else -> array() // can't infer anything
    }
}

private fun JetNamedDeclaration.guessType(context: BindingContext): Array<JetType> {
    val scope = getContainingFile()!!.getUseScope()
    val expectedTypes = SearchUtils.findAllReferences(this, scope)!!.stream().map { ref ->
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
    if (when (variance) {
        Variance.INVARIANT      -> this == substitution.forType
        Variance.IN_VARIANCE    -> JetTypeChecker.DEFAULT.isSubtypeOf(this, substitution.forType)
        Variance.OUT_VARIANCE   -> JetTypeChecker.DEFAULT.isSubtypeOf(substitution.forType, this)
    }) {
        return substitution.byType
    }
    else {
        val newArguments = getArguments().zip(getConstructor().getParameters()).map { pair ->
            val (projection, typeParameter) = pair
            TypeProjectionImpl(Variance.INVARIANT, projection.getType().substitute(substitution, typeParameter.getVariance()))
        }
        return JetTypeImpl(getAnnotations(), getConstructor(), isNullable(), newArguments, getMemberScope())
    }
}