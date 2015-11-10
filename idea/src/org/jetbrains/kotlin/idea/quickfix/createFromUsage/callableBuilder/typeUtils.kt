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

import com.intellij.refactoring.psi.SearchUtils
import org.jetbrains.kotlin.cfg.pseudocode.*
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.idea.project.platform
import org.jetbrains.kotlin.idea.references.KtSimpleNameReference
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getAssignmentByLHS
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.bindingContextUtil.isUsedAsStatement
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValueFactory
import org.jetbrains.kotlin.resolve.descriptorUtil.resolveTopLevelClass
import org.jetbrains.kotlin.resolve.scopes.HierarchicalScope
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.resolve.scopes.utils.findClassifier
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker
import org.jetbrains.kotlin.types.typeUtil.makeNotNullable
import java.util.*

internal fun KotlinType.contains(inner: KotlinType): Boolean {
    return KotlinTypeChecker.DEFAULT.equalTypes(this, inner) || getArguments().any { inner in it.getType() }
}

private fun KotlinType.render(typeParameterNameMap: Map<TypeParameterDescriptor, String>, fq: Boolean): String {
    val substitution = typeParameterNameMap
            .mapValues {
                val name = Name.identifier(it.value)

                val typeParameter = it.key

                var wrappingTypeParameter: TypeParameterDescriptor
                var wrappingTypeConstructor: TypeConstructor

                wrappingTypeParameter = object : TypeParameterDescriptor by typeParameter {
                    override fun getName() = name
                    override fun getTypeConstructor() = wrappingTypeConstructor
                }

                wrappingTypeConstructor = object : TypeConstructor by typeParameter.typeConstructor {
                    override fun getDeclarationDescriptor() = wrappingTypeParameter
                }

                val wrappingType = object : KotlinType by typeParameter.defaultType {
                    override fun getConstructor() = wrappingTypeConstructor
                }

                TypeProjectionImpl(wrappingType)
            }
            .mapKeys { it.key.typeConstructor }

    val typeToRender = TypeSubstitutor.create(substitution).substitute(this, Variance.INVARIANT)!!
    val renderer = if (fq) IdeDescriptorRenderers.SOURCE_CODE else IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_IN_TYPES
    return renderer.renderType(typeToRender)
}

internal fun KotlinType.renderShort(typeParameterNameMap: Map<TypeParameterDescriptor, String>) = render(typeParameterNameMap, false)
internal fun KotlinType.renderLong(typeParameterNameMap: Map<TypeParameterDescriptor, String>) = render(typeParameterNameMap, true)

internal fun getTypeParameterNamesNotInScope(typeParameters: Collection<TypeParameterDescriptor>, scope: HierarchicalScope): List<TypeParameterDescriptor> {
    return typeParameters.filter { typeParameter ->
        val classifier = scope.findClassifier(typeParameter.name, NoLookupLocation.FROM_IDE)
        classifier == null || classifier != typeParameter
    }
}

fun KotlinType.containsStarProjections(): Boolean = arguments.any { it.isStarProjection || it.type.containsStarProjections() }

fun KotlinType.getTypeParameters(): Set<TypeParameterDescriptor> {
    val visitedTypes = HashSet<KotlinType>()
    val typeParameters = LinkedHashSet<TypeParameterDescriptor>()

    fun traverseTypes(type: KotlinType) {
        if (!visitedTypes.add(type)) return

        val arguments = type.arguments
        if (arguments.isEmpty()) {
            val descriptor = type.constructor.declarationDescriptor
            if (descriptor is TypeParameterDescriptor) {
                typeParameters.add(descriptor)
            }
            return
        }

        arguments.forEach { traverseTypes(it.type) }
    }

    traverseTypes(this)
    return typeParameters
}

fun KtExpression.guessTypes(
        context: BindingContext,
        module: ModuleDescriptor,
        pseudocode: Pseudocode? = null,
        coerceUnusedToUnit: Boolean = true
): Array<KotlinType> {
    if (coerceUnusedToUnit
        && this !is KtDeclaration
        && isUsedAsStatement(context)
        && getNonStrictParentOfType<KtAnnotationEntry>() == null) return arrayOf(module.builtIns.getUnitType())

    // if we know the actual type of the expression
    val theType1 = context.getType(this)
    if (theType1 != null) {
        val dataFlowInfo = context[BindingContext.EXPRESSION_TYPE_INFO, this]?.dataFlowInfo
        val possibleTypes = dataFlowInfo?.getPossibleTypes(DataFlowValueFactory.createDataFlowValue(this, theType1, context, module))
        return if (possibleTypes != null && possibleTypes.isNotEmpty()) possibleTypes.toTypedArray() else arrayOf(theType1)
    }

    // expression has an expected type
    val theType2 = context[BindingContext.EXPECTED_EXPRESSION_TYPE, this]
    if (theType2 != null) return arrayOf(theType2)

    val parent = getParent()
    return when {
        this is KtTypeConstraint -> {
            // expression itself is a type assertion
            val constraint = this
            arrayOf(context[BindingContext.TYPE, constraint.getBoundTypeReference()]!!)
        }
        parent is KtTypeConstraint -> {
            // expression is on the left side of a type assertion
            val constraint = parent
            arrayOf(context[BindingContext.TYPE, constraint.getBoundTypeReference()]!!)
        }
        this is KtMultiDeclarationEntry -> {
            // expression is on the lhs of a multi-declaration
            val typeRef = getTypeReference()
            if (typeRef != null) {
                // and has a specified type
                arrayOf(context[BindingContext.TYPE, typeRef]!!)
            }
            else {
                // otherwise guess
                guessType(context)
            }
        }
        this is KtParameter -> {
            // expression is a parameter (e.g. declared in a for-loop)
            val typeRef = getTypeReference()
            if (typeRef != null) {
                // and has a specified type
                arrayOf(context[BindingContext.TYPE, typeRef]!!)
            }
            else {
                // otherwise guess
                guessType(context)
            }
        }
        parent is KtProperty && parent.isLocal() -> {
            // the expression is the RHS of a variable assignment with a specified type
            val variable = parent
            val typeRef = variable.getTypeReference()
            if (typeRef != null) {
                // and has a specified type
                arrayOf(context[BindingContext.TYPE, typeRef]!!)
            }
            else {
                // otherwise guess, based on LHS
                variable.guessType(context)
            }
        }
        parent is KtPropertyDelegate -> {
            val variableDescriptor = context[BindingContext.DECLARATION_TO_DESCRIPTOR, parent.getParent() as KtProperty] as VariableDescriptor
            val delegateClassName = if (variableDescriptor.isVar()) "ReadWriteProperty" else "ReadOnlyProperty"
            val delegateClass = module.resolveTopLevelClass(FqName("kotlin.properties.$delegateClassName"), NoLookupLocation.FROM_IDE)
                                ?: return arrayOf(module.builtIns.getAnyType())
            val receiverType = (variableDescriptor.getExtensionReceiverParameter() ?: variableDescriptor.getDispatchReceiverParameter())?.getType()
                               ?: module.builtIns.getNullableNothingType()
            val typeArguments = listOf(TypeProjectionImpl(receiverType), TypeProjectionImpl(variableDescriptor.getType()))
            arrayOf(TypeUtils.substituteProjectionsForParameters(delegateClass, typeArguments))
        }
        parent is KtStringTemplateEntryWithExpression && parent.getExpression() == this -> {
            arrayOf(module.builtIns.getStringType())
        }
        else -> {
            pseudocode?.getElementValue(this)?.let {
                getExpectedTypePredicate(it, context, module.builtIns).getRepresentativeTypes().toTypedArray()
            } ?: arrayOf() // can't infer anything
        }
    }
}

private fun KtNamedDeclaration.guessType(context: BindingContext): Array<KotlinType> {
    val expectedTypes = SearchUtils.findAllReferences(this, getUseScope())!!.asSequence().map { ref ->
        if (ref is KtSimpleNameReference) {
            context[BindingContext.EXPECTED_EXPRESSION_TYPE, ref.expression]
        }
        else {
            null
        }
    }.filterNotNullTo(HashSet<KotlinType>())

    if (expectedTypes.isEmpty() || expectedTypes.any { expectedType -> ErrorUtils.containsErrorType(expectedType) }) {
        return arrayOf()
    }
    val theType = TypeIntersector.intersectTypes(KotlinTypeChecker.DEFAULT, expectedTypes)
    if (theType != null) {
        return arrayOf(theType)
    }
    else {
        // intersection doesn't exist; let user make an imperfect choice
        return expectedTypes.toTypedArray()
    }
}

/**
 * Encapsulates a single type substitution of a <code>KotlinType</code> by another <code>KotlinType</code>.
 */
internal class KotlinTypeSubstitution(public val forType: KotlinType, public val byType: KotlinType)

internal fun KotlinType.substitute(substitution: KotlinTypeSubstitution, variance: Variance): KotlinType {
    val nullable = isMarkedNullable()
    val currentType = makeNotNullable()

    if (when (variance) {
        Variance.INVARIANT      -> KotlinTypeChecker.DEFAULT.equalTypes(currentType, substitution.forType)
        Variance.IN_VARIANCE    -> KotlinTypeChecker.DEFAULT.isSubtypeOf(currentType, substitution.forType)
        Variance.OUT_VARIANCE   -> KotlinTypeChecker.DEFAULT.isSubtypeOf(substitution.forType, currentType)
    }) {
        return TypeUtils.makeNullableAsSpecified(substitution.byType, nullable)
    }
    else {
        val newArguments = getArguments().zip(getConstructor().getParameters()).map { pair ->
            val (projection, typeParameter) = pair
            TypeProjectionImpl(Variance.INVARIANT, projection.getType().substitute(substitution, typeParameter.getVariance()))
        }
        return KotlinTypeImpl.create(getAnnotations(), getConstructor(), isMarkedNullable(), newArguments, getMemberScope())
    }
}

fun KtExpression.getExpressionForTypeGuess() = getAssignmentByLHS()?.getRight() ?: this

fun KtCallElement.getTypeInfoForTypeArguments(): List<TypeInfo> {
    return getTypeArguments().map { it.getTypeReference()?.let { TypeInfo(it, Variance.INVARIANT) } }.filterNotNull()
}

fun KtCallExpression.getParameterInfos(): List<ParameterInfo> {
    val anyType = this.platform.builtIns.nullableAnyType
    return valueArguments.map {
        ParameterInfo(
                it.getArgumentExpression()?.let { TypeInfo(it, Variance.IN_VARIANCE) } ?: TypeInfo(anyType, Variance.IN_VARIANCE),
                it.getArgumentName()?.referenceExpression?.getReferencedName()
        )
    }
}

private fun TypePredicate.getRepresentativeTypes(): Set<KotlinType> {
    return when (this) {
        is SingleType -> Collections.singleton(targetType)
        is AllSubtypes -> Collections.singleton(upperBound)
        is ForAllTypes -> {
            if (typeSets.isEmpty()) AllTypes.getRepresentativeTypes()
            else typeSets.map { it.getRepresentativeTypes() }.reduce { a, b -> a intersect b }
        }
        is ForSomeType -> typeSets.flatMapTo(LinkedHashSet<KotlinType>()) { it.getRepresentativeTypes() }
        is AllTypes -> emptySet()
        else -> throw AssertionError("Invalid type predicate: ${this}")
    }
}
