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

package org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder

import com.intellij.refactoring.psi.SearchUtils
import org.jetbrains.kotlin.builtins.isFunctionType
import org.jetbrains.kotlin.cfg.pseudocode.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.project.builtIns
import org.jetbrains.kotlin.idea.references.KtSimpleNameReference
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getAssignmentByLHS
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.bindingContextUtil.getDataFlowInfoAfter
import org.jetbrains.kotlin.resolve.bindingContextUtil.isUsedAsStatement
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValueFactory
import org.jetbrains.kotlin.resolve.descriptorUtil.resolveTopLevelClass
import org.jetbrains.kotlin.resolve.scopes.HierarchicalScope
import org.jetbrains.kotlin.resolve.scopes.utils.findClassifier
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker
import org.jetbrains.kotlin.types.typeUtil.makeNotNullable
import org.jetbrains.kotlin.types.typeUtil.supertypes
import java.util.*

internal operator fun KotlinType.contains(inner: KotlinType): Boolean {
    return KotlinTypeChecker.DEFAULT.equalTypes(this, inner) || arguments.any { inner in it.type }
}

internal operator fun KotlinType.contains(descriptor: ClassifierDescriptor): Boolean {
    return constructor.declarationDescriptor == descriptor || arguments.any { descriptor in it.type }
}

internal fun KotlinType.decomposeIntersection(): List<KotlinType> {
    (constructor as? IntersectionTypeConstructor)?.let {
        return it.supertypes.flatMap { it.decomposeIntersection() }
    }

    return listOf(this)
}

private fun KotlinType.renderSingle(typeParameterNameMap: Map<TypeParameterDescriptor, String>, fq: Boolean): String {
    val substitution = typeParameterNameMap
            .mapValues {
                val name = Name.identifier(it.value)

                val typeParameter = it.key

                var wrappingTypeParameter: TypeParameterDescriptor? = null
                val wrappingTypeConstructor = object : TypeConstructor by typeParameter.typeConstructor {
                    override fun getDeclarationDescriptor() = wrappingTypeParameter
                }

                wrappingTypeParameter = object : TypeParameterDescriptor by typeParameter {
                    override fun getName() = name
                    override fun getTypeConstructor() = wrappingTypeConstructor
                }

                val wrappingType = KotlinTypeFactory.simpleType(typeParameter.defaultType, constructor = wrappingTypeConstructor)
                TypeProjectionImpl(wrappingType)
            }
            .mapKeys { it.key.typeConstructor }

    val typeToRender = TypeSubstitutor.create(substitution).substitute(this, Variance.INVARIANT)!!
    val renderer = if (fq) IdeDescriptorRenderers.SOURCE_CODE else IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_IN_TYPES
    return renderer.renderType(typeToRender)
}

private fun KotlinType.render(typeParameterNameMap: Map<TypeParameterDescriptor, String>, fq: Boolean): List<String> {
    return decomposeIntersection().map { it.renderSingle(typeParameterNameMap, fq) }
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
        coerceUnusedToUnit: Boolean = true,
        allowErrorTypes: Boolean = false
): Array<KotlinType> {
    fun isAcceptable(type: KotlinType) = allowErrorTypes || !ErrorUtils.containsErrorType(type)

    if (coerceUnusedToUnit
        && this !is KtDeclaration
        && isUsedAsStatement(context)
        && getNonStrictParentOfType<KtAnnotationEntry>() == null) return arrayOf(module.builtIns.unitType)

    // if we know the actual type of the expression
    val theType1 = context.getType(this)
    if (theType1 != null && isAcceptable(theType1)) {
        val dataFlowInfo = context.getDataFlowInfoAfter(this)
        val possibleTypes = dataFlowInfo.getCollectedTypes(DataFlowValueFactory.createDataFlowValue(this, theType1, context, module))
        return if (possibleTypes.isNotEmpty()) possibleTypes.toTypedArray() else arrayOf(theType1)
    }

    // expression has an expected type
    val theType2 = context[BindingContext.EXPECTED_EXPRESSION_TYPE, this]
    if (theType2 != null && isAcceptable(theType2)) return arrayOf(theType2)

    val parent = parent
    return when {
        this is KtTypeConstraint -> {
            // expression itself is a type assertion
            val constraint = this
            arrayOf(context[BindingContext.TYPE, constraint.boundTypeReference]!!)
        }
        parent is KtTypeConstraint -> {
            // expression is on the left side of a type assertion
            arrayOf(context[BindingContext.TYPE, parent.boundTypeReference]!!)
        }
        this is KtDestructuringDeclarationEntry -> {
            // expression is on the lhs of a multi-declaration
            val typeRef = typeReference
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
            val typeRef = typeReference
            if (typeRef != null) {
                // and has a specified type
                arrayOf(context[BindingContext.TYPE, typeRef]!!)
            }
            else {
                // otherwise guess
                guessType(context)
            }
        }
        parent is KtProperty && parent.isLocal -> {
            // the expression is the RHS of a variable assignment with a specified type
            val typeRef = parent.typeReference
            if (typeRef != null) {
                // and has a specified type
                arrayOf(context[BindingContext.TYPE, typeRef]!!)
            }
            else {
                // otherwise guess, based on LHS
                parent.guessType(context)
            }
        }
        parent is KtPropertyDelegate -> {
            val variableDescriptor = context[BindingContext.DECLARATION_TO_DESCRIPTOR, parent.parent as KtProperty] as VariableDescriptor
            val delegateClassName = if (variableDescriptor.isVar) "ReadWriteProperty" else "ReadOnlyProperty"
            val delegateClass = module.resolveTopLevelClass(FqName("kotlin.properties.$delegateClassName"), NoLookupLocation.FROM_IDE)
                                ?: return arrayOf(module.builtIns.anyType)
            val receiverType = (variableDescriptor.extensionReceiverParameter ?: variableDescriptor.dispatchReceiverParameter)?.type
                               ?: module.builtIns.nullableNothingType
            val typeArguments = listOf(TypeProjectionImpl(receiverType), TypeProjectionImpl(variableDescriptor.type))
            arrayOf(TypeUtils.substituteProjectionsForParameters(delegateClass, typeArguments))
        }
        parent is KtStringTemplateEntryWithExpression && parent.expression == this -> {
            arrayOf(module.builtIns.stringType)
        }
        parent is KtBlockExpression && parent.statements.lastOrNull() == this && parent.parent is KtFunctionLiteral -> {
            parent.guessTypes(context, module, pseudocode, coerceUnusedToUnit)
        }
        parent is KtFunction -> {
            val functionDescriptor = context[BindingContext.DECLARATION_TO_DESCRIPTOR, parent] as? FunctionDescriptor ?: return arrayOf()
            val returnType = functionDescriptor.returnType
            if (returnType != null && isAcceptable(returnType)) return arrayOf(returnType)
            val functionalExpression: KtExpression? = when {
                parent is KtFunctionLiteral -> parent.parent as? KtLambdaExpression
                parent is KtNamedFunction && parent.name == null -> parent
                else -> null
            }
            if (functionalExpression == null) {
                functionDescriptor.overriddenDescriptors
                        .mapNotNull { it.returnType }
                        .firstOrNull { isAcceptable(it) }
                        ?.let { return arrayOf(it) }
                return arrayOf()
            }
            val lambdaTypes = functionalExpression.guessTypes(context, module, pseudocode?.parent, coerceUnusedToUnit)
            lambdaTypes.mapNotNull { it.getFunctionType()?.arguments?.lastOrNull()?.type }.toTypedArray()
        }
        else -> {
            pseudocode?.getElementValue(this)?.let {
                getExpectedTypePredicate(it, context, module.builtIns).getRepresentativeTypes().toTypedArray()
            } ?: arrayOf() // can't infer anything
        }
    }
}

private fun KotlinType.getFunctionType() = if (isFunctionType) this else supertypes().firstOrNull { it.isFunctionType }

private fun KtNamedDeclaration.guessType(context: BindingContext): Array<KotlinType> {
    val expectedTypes = SearchUtils.findAllReferences(this, useScope)!!.mapNotNullTo(HashSet<KotlinType>()) { ref ->
        if (ref is KtSimpleNameReference) {
            context[BindingContext.EXPECTED_EXPRESSION_TYPE, ref.expression]
        }
        else {
            null
        }
    }

    if (expectedTypes.isEmpty() || expectedTypes.any { expectedType -> ErrorUtils.containsErrorType(expectedType) }) {
        return arrayOf()
    }
    val theType = TypeIntersector.intersectTypes(KotlinTypeChecker.DEFAULT, expectedTypes)
    return if (theType != null) {
        arrayOf(theType)
    }
    else {
        // intersection doesn't exist; let user make an imperfect choice
        expectedTypes.toTypedArray()
    }
}

/**
 * Encapsulates a single type substitution of a <code>KotlinType</code> by another <code>KotlinType</code>.
 */
internal class KotlinTypeSubstitution(val forType: KotlinType, val byType: KotlinType)

internal fun KotlinType.substitute(substitution: KotlinTypeSubstitution, variance: Variance): KotlinType {
    val nullable = isMarkedNullable
    val currentType = makeNotNullable()

    return if (when (variance) {
        Variance.INVARIANT      -> KotlinTypeChecker.DEFAULT.equalTypes(currentType, substitution.forType)
        Variance.IN_VARIANCE    -> KotlinTypeChecker.DEFAULT.isSubtypeOf(currentType, substitution.forType)
        Variance.OUT_VARIANCE   -> KotlinTypeChecker.DEFAULT.isSubtypeOf(substitution.forType, currentType)
    }) {
        TypeUtils.makeNullableAsSpecified(substitution.byType, nullable)
    }
    else {
        val newArguments = arguments.zip(constructor.parameters).map { pair ->
            val (projection, typeParameter) = pair
            TypeProjectionImpl(Variance.INVARIANT, projection.type.substitute(substitution, typeParameter.variance))
        }
        KotlinTypeFactory.simpleType(annotations, constructor, newArguments, isMarkedNullable, memberScope)
    }
}

fun KtExpression.getExpressionForTypeGuess() = getAssignmentByLHS()?.right ?: this

fun KtCallElement.getTypeInfoForTypeArguments(): List<TypeInfo> {
    return typeArguments.mapNotNull { it.typeReference?.let { TypeInfo(it, Variance.INVARIANT) } }
}

fun KtCallExpression.getParameterInfos(): List<ParameterInfo> {
    val anyType = this.builtIns.nullableAnyType
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
            else typeSets.map { it.getRepresentativeTypes() }.reduce { a, b -> a.intersect(b) }
        }
        is ForSomeType -> typeSets.flatMapTo(LinkedHashSet<KotlinType>()) { it.getRepresentativeTypes() }
        is AllTypes -> emptySet()
        else -> throw AssertionError("Invalid type predicate: ${this}")
    }
}
