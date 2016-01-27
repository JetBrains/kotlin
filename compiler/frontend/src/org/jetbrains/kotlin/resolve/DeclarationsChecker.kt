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

package org.jetbrains.kotlin.resolve

import com.google.common.collect.ImmutableSet
import com.google.common.collect.Sets
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory0
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.diagnostics.Errors.*
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext.TYPE
import org.jetbrains.kotlin.resolve.BindingContext.TYPE_PARAMETER
import org.jetbrains.kotlin.resolve.DescriptorUtils.classCanHaveAbstractMembers
import org.jetbrains.kotlin.resolve.DescriptorUtils.classCanHaveOpenMembers
import org.jetbrains.kotlin.types.IntersectionTypeConstructor
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.SubstitutionUtils
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker
import org.jetbrains.kotlin.types.typeUtil.isNothing
import java.util.*

fun KtDeclaration.checkTypeReferences(trace: BindingTrace) {
    if (this is KtCallableDeclaration) {
        typeReference?.checkNotEnumEntry(trace)
        receiverTypeReference?.checkNotEnumEntry(trace)
    }
    if (this is KtDeclarationWithBody) {
        for (parameter in valueParameters) {
            parameter.typeReference?.checkNotEnumEntry(trace)
        }
    }
}

fun KtTypeReference.checkNotEnumEntry(trace: BindingTrace): Boolean {
    var result = false
    trace.bindingContext.get(TYPE, this)?.let {
        val targetDescriptor = TypeUtils.getClassDescriptor(it)
        if (targetDescriptor != null && DescriptorUtils.isEnumEntry(targetDescriptor)) {
            trace.report(ENUM_ENTRY_AS_TYPE.on(this))
            result = true
        }
    }
    typeElement?.let {
        for (typeArgument in it.typeArgumentsAsTypes) {
            typeArgument?.checkNotEnumEntry(trace)
        }
    }
    return result
}

internal class DeclarationsCheckerBuilder(
        private val descriptorResolver: DescriptorResolver,
        private val originalModifiersChecker: ModifiersChecker,
        private val annotationChecker: AnnotationChecker,
        private val identifierChecker: IdentifierChecker
) {
    fun withTrace(trace: BindingTrace) =
            DeclarationsChecker(descriptorResolver, originalModifiersChecker, annotationChecker, identifierChecker, trace)
}

class DeclarationsChecker(
        private val descriptorResolver: DescriptorResolver,
        modifiersChecker: ModifiersChecker,
        private val annotationChecker: AnnotationChecker,
        private val identifierChecker: IdentifierChecker,
        private val trace: BindingTrace
) {

    private val modifiersChecker = modifiersChecker.withTrace(trace)

    fun KtDeclaration.checkTypeReferences() = checkTypeReferences(trace)

    fun process(bodiesResolveContext: BodiesResolveContext) {
        for (file in bodiesResolveContext.files) {
            checkModifiersAndAnnotationsInPackageDirective(file)
            annotationChecker.check(file, trace, null)
        }

        for ((classOrObject, classDescriptor) in bodiesResolveContext.declaredClasses.entries) {
            checkSupertypesForConsistency(classDescriptor, classOrObject)
            checkTypesInClassHeader(classOrObject)
            checkClassOrObjectMembers(classDescriptor)

            when (classOrObject) {
                is KtClass -> {
                    checkClassButNotObject(classOrObject, classDescriptor)
                    descriptorResolver.checkNamesInConstraints(
                            classOrObject, classDescriptor, classDescriptor.scopeForClassHeaderResolution, trace)
                }
                is KtObjectDeclaration -> {
                    checkObject(classOrObject, classDescriptor)
                }
            }

            checkPrimaryConstructor(classOrObject, classDescriptor)

            classOrObject.checkTypeReferences()
            modifiersChecker.checkModifiersForDeclaration(classOrObject, classDescriptor)
            identifierChecker.checkDeclaration(classOrObject, trace)
            checkClassExposedType(classOrObject, classDescriptor)
        }

        for ((function, functionDescriptor) in bodiesResolveContext.functions.entries) {
            checkFunction(function, functionDescriptor)
            function.checkTypeReferences()
            modifiersChecker.checkModifiersForDeclaration(function, functionDescriptor)
            identifierChecker.checkDeclaration(function, trace)
        }

        for ((property, propertyDescriptor) in bodiesResolveContext.properties.entries) {
            checkProperty(property, propertyDescriptor)
            property.checkTypeReferences()
            modifiersChecker.checkModifiersForDeclaration(property, propertyDescriptor)
            identifierChecker.checkDeclaration(property, trace)
        }

        for ((declaration, constructorDescriptor) in bodiesResolveContext.secondaryConstructors.entries) {
            checkConstructorDeclaration(constructorDescriptor, declaration)
            checkFunctionExposedType(declaration, constructorDescriptor)
        }
    }

    private fun checkConstructorDeclaration(constructorDescriptor: ConstructorDescriptor, declaration: KtDeclaration) {
        declaration.checkTypeReferences()
        modifiersChecker.checkModifiersForDeclaration(declaration, constructorDescriptor)
        identifierChecker.checkDeclaration(declaration, trace)
        checkVarargParameters(trace, constructorDescriptor)
    }

    private fun checkModifiersAndAnnotationsInPackageDirective(file: KtFile) {
        val packageDirective = file.packageDirective ?: return
        val modifierList = packageDirective.modifierList ?: return

        for (annotationEntry in modifierList.annotationEntries) {
            val calleeExpression = annotationEntry.calleeExpression
            if (calleeExpression != null) {
                calleeExpression.constructorReferenceExpression?.let { trace.report(UNRESOLVED_REFERENCE.on(it, it)) }
            }
        }
        annotationChecker.check(packageDirective, trace, null)
        ModifierCheckerCore.check(packageDirective, trace, null)
    }

    private fun checkTypesInClassHeader(classOrObject: KtClassOrObject) {
        fun KtTypeReference.type(): KotlinType? = trace.bindingContext.get(TYPE, this)

        for (delegationSpecifier in classOrObject.getSuperTypeListEntries()) {
            val typeReference = delegationSpecifier.typeReference ?: continue
            typeReference.type()?.let { DescriptorResolver.checkBounds(typeReference, it, trace) }
            typeReference.checkNotEnumEntry(trace)
        }

        if (classOrObject !is KtClass) return

        val upperBoundCheckRequests = ArrayList<DescriptorResolver.UpperBoundCheckRequest>()

        for (typeParameter in classOrObject.typeParameters) {
            val typeReference = typeParameter.extendsBound ?: continue
            val type = typeReference.type() ?: continue
            upperBoundCheckRequests.add(DescriptorResolver.UpperBoundCheckRequest(typeParameter.nameAsName, typeReference, type))
        }

        for (constraint in classOrObject.typeConstraints) {
            val typeReference = constraint.boundTypeReference ?: continue
            val type = typeReference.type() ?: continue
            val name = constraint.subjectTypeParameterName?.getReferencedNameAsName() ?: continue
            upperBoundCheckRequests.add(DescriptorResolver.UpperBoundCheckRequest(name, typeReference, type))
        }

        DescriptorResolver.checkUpperBoundTypes(trace, upperBoundCheckRequests)

        for (request in upperBoundCheckRequests) {
            DescriptorResolver.checkBounds(request.upperBound, request.upperBoundType, trace)
        }
    }

    private fun checkOnlyOneTypeParameterBound(
            descriptor: TypeParameterDescriptor, declaration: KtTypeParameter, owner: KtTypeParameterListOwner
    ) {
        val upperBounds = descriptor.upperBounds
        val (boundsWhichAreTypeParameters, otherBounds) = upperBounds
                .map { type -> type.constructor }
                .partition { constructor -> constructor.declarationDescriptor is TypeParameterDescriptor }
                .let { pair -> pair.first.toSet() to pair.second.toSet() }
        if (boundsWhichAreTypeParameters.size > 1 || (boundsWhichAreTypeParameters.size == 1 && otherBounds.isNotEmpty())) {
            val reportOn = if (boundsWhichAreTypeParameters.size + otherBounds.size == 2) {
                // If there's only one problematic bound (either 2 type parameter bounds, or 1 type parameter bound + 1 other bound),
                // report the diagnostic on that bound

                val allBounds: List<Pair<KtTypeReference, KotlinType?>> =
                        owner.typeConstraints
                                .filter { constraint ->
                                    constraint.subjectTypeParameterName?.getReferencedNameAsName() == declaration.nameAsName
                                }
                                .mapNotNull { constraint -> constraint.boundTypeReference }
                                .map { typeReference -> typeReference to trace.bindingContext.get(TYPE, typeReference) }

                val problematicBound =
                        allBounds.firstOrNull { bound -> bound.second?.constructor != boundsWhichAreTypeParameters.first() }

                problematicBound?.first ?: declaration
            }
            else {
                // Otherwise report the diagnostic on the type parameter declaration
                declaration
            }

            trace.report(BOUNDS_NOT_ALLOWED_IF_BOUNDED_BY_TYPE_PARAMETER.on(reportOn))
        }
    }

    private fun checkSupertypesForConsistency(classifier: ClassifierDescriptor, sourceElement: PsiElement) {
        if (classifier is TypeParameterDescriptor) {
            val immediateUpperBounds = classifier.upperBounds.map { it.constructor }
            if (immediateUpperBounds.size != immediateUpperBounds.toSet().size) {
                // If there are duplicate type constructors among the _immediate_ upper bounds,
                // then the REPEATED_BOUNDS diagnostic would be already reported for those bounds of this type parameter
                return
            }
        }

        val multiMap = SubstitutionUtils.buildDeepSubstitutionMultimap(classifier.defaultType)
        for ((typeParameterDescriptor, projections) in multiMap.asMap()) {
            if (projections.size <= 1) continue

            // Immediate arguments of supertypes cannot be projected
            val conflictingTypes = projections.map { it.type }.toMutableSet()
            removeDuplicateTypes(conflictingTypes)
            if (conflictingTypes.size <= 1) continue

            val containingDeclaration = typeParameterDescriptor.containingDeclaration as? ClassDescriptor
                                        ?: throw AssertionError("Not a class descriptor: " + typeParameterDescriptor.containingDeclaration)
            if (sourceElement is KtClassOrObject) {
                val delegationSpecifierList = sourceElement.getSuperTypeList() ?: continue
                trace.report(INCONSISTENT_TYPE_PARAMETER_VALUES.on(
                        delegationSpecifierList, typeParameterDescriptor, containingDeclaration, conflictingTypes
                ))
            }
            else if (sourceElement is KtTypeParameter) {
                trace.report(INCONSISTENT_TYPE_PARAMETER_BOUNDS.on(
                        sourceElement, typeParameterDescriptor, containingDeclaration, conflictingTypes
                ))
            }
        }
    }

    private fun checkClassExposedType(klass: KtClassOrObject, classDescriptor: ClassDescriptor) {
        checkExposedSupertypes(klass, classDescriptor)
        checkExposedParameterBounds(klass, classDescriptor)

        val constructor = klass.getPrimaryConstructor() ?: return
        val constructorDescriptor = classDescriptor.unsubstitutedPrimaryConstructor ?: return
        checkFunctionExposedType(constructor, constructorDescriptor)
    }

    private fun checkExposedParameterBounds(klass: KtClassOrObject, classDescriptor: ClassDescriptor) {
        val classVisibility = classDescriptor.effectiveVisibility()
        val typeParameterList = klass.typeParameters
        classDescriptor.declaredTypeParameters.forEachIndexed { i, typeParameterDescriptor ->
            if (i >= typeParameterList.size) return
            for (upperBound in typeParameterDescriptor.upperBounds) {
                val upperBoundVisibility = upperBound.effectiveVisibility()
                if (!upperBoundVisibility.sameOrMorePermissive(classVisibility)) {
                    trace.report(EXPOSED_TYPE_PARAMETER_BOUND.on(typeParameterList[i], classVisibility, upperBoundVisibility))
                    break
                }
            }
        }
    }

    private fun checkExposedSupertypes(klass: KtClassOrObject, classDescriptor: ClassDescriptor) {
        val classVisibility = classDescriptor.effectiveVisibility()
        val isInterface = classDescriptor.kind == ClassKind.INTERFACE
        val delegationList = klass.getSuperTypeListEntries()
        classDescriptor.typeConstructor.supertypes.forEachIndexed { i, superType ->
            if (i >= delegationList.size) return
            val superDescriptor = TypeUtils.getClassDescriptor(superType) ?: return@forEachIndexed
            val superIsInterface = superDescriptor.kind == ClassKind.INTERFACE
            if (superIsInterface != isInterface) {
                return@forEachIndexed
            }
            val superTypeVisibility = superType.effectiveVisibility()
            if (!superTypeVisibility.sameOrMorePermissive(classVisibility)) {
                if (isInterface) {
                    trace.report(EXPOSED_SUPER_INTERFACE.on(delegationList[i], classVisibility, superTypeVisibility))
                }
                else {
                    trace.report(EXPOSED_SUPER_CLASS.on(delegationList[i], classVisibility, superTypeVisibility))
                }
            }
        }
    }

    private fun checkObject(declaration: KtObjectDeclaration, classDescriptor: ClassDescriptorWithResolutionScopes) {
        checkOpenMembers(classDescriptor)
        if (declaration.isLocal() && !declaration.isCompanion() && !declaration.isObjectLiteral()) {
            trace.report(LOCAL_OBJECT_NOT_ALLOWED.on(declaration, classDescriptor))
        }
    }

    private fun checkClassButNotObject(aClass: KtClass, classDescriptor: ClassDescriptorWithResolutionScopes) {
        checkOpenMembers(classDescriptor)
        checkTypeParameters(aClass)
        checkTypeParameterConstraints(aClass)
        FiniteBoundRestrictionChecker.check(aClass, classDescriptor, trace)
        NonExpansiveInheritanceRestrictionChecker.check(aClass, classDescriptor, trace)

        if (aClass.isInterface()) {
            checkConstructorInInterface(aClass)
            checkMethodsOfAnyInInterface(classDescriptor)
            if (aClass.isLocal() && classDescriptor.containingDeclaration !is ClassDescriptor) {
                trace.report(LOCAL_INTERFACE_NOT_ALLOWED.on(aClass, classDescriptor))
            }
        }
        else if (classDescriptor.kind == ClassKind.ANNOTATION_CLASS) {
            checkAnnotationClassWithBody(aClass)
            checkValOnAnnotationParameter(aClass)
        }
        else if (aClass is KtEnumEntry) {
            checkEnumEntry(aClass, classDescriptor)
        }
    }

    private fun checkClassOrObjectMembers(classDescriptor: ClassDescriptorWithResolutionScopes) {
        for (memberDescriptor in classDescriptor.declaredCallableMembers) {
            if (memberDescriptor.kind != CallableMemberDescriptor.Kind.DECLARATION) continue
            val member = DescriptorToSourceUtils.descriptorToDeclaration(memberDescriptor) as? KtFunction
            if (member != null && memberDescriptor is FunctionDescriptor) {
                checkFunctionExposedType(member, memberDescriptor)
                checkVarargParameters(trace, memberDescriptor)
            }
        }
    }

    private fun checkPrimaryConstructor(classOrObject: KtClassOrObject, classDescriptor: ClassDescriptor) {
        val primaryConstructor = classDescriptor.unsubstitutedPrimaryConstructor ?: return
        val declaration = classOrObject.getPrimaryConstructor() ?: return

        for (parameter in declaration.valueParameters) {
            trace.get(BindingContext.PRIMARY_CONSTRUCTOR_PARAMETER, parameter)?.let {
                modifiersChecker.checkModifiersForDeclaration(parameter, it)
                checkPropertyLateInit(parameter, it)
            }
        }

        if (!declaration.hasConstructorKeyword()) {
            declaration.modifierList?.let { trace.report(MISSING_CONSTRUCTOR_KEYWORD.on(it)) }
        }

        if (classOrObject !is KtClass) {
            trace.report(CONSTRUCTOR_IN_OBJECT.on(declaration))
        }

        checkConstructorDeclaration(primaryConstructor, declaration)
    }

    private fun checkTypeParameters(typeParameterListOwner: KtTypeParameterListOwner) {
        // TODO: Support annotation for type parameters
        for (jetTypeParameter in typeParameterListOwner.typeParameters) {
            AnnotationResolver.reportUnsupportedAnnotationForTypeParameter(jetTypeParameter, trace)

            trace.get(TYPE_PARAMETER, jetTypeParameter)?.let { DescriptorResolver.checkConflictingUpperBounds(trace, it, jetTypeParameter) }
        }
    }

    private fun checkTypeParameterConstraints(typeParameterListOwner: KtTypeParameterListOwner) {
        val constraints = typeParameterListOwner.typeConstraints
        if (constraints.isEmpty()) return

        for (typeParameter in typeParameterListOwner.typeParameters) {
            if (typeParameter.extendsBound != null && hasConstraints(typeParameter, constraints)) {
                trace.report(MISPLACED_TYPE_PARAMETER_CONSTRAINTS.on(typeParameter))
            }
            val typeParameterDescriptor = trace.get(TYPE_PARAMETER, typeParameter) ?: continue
            checkSupertypesForConsistency(typeParameterDescriptor, typeParameter)
            checkOnlyOneTypeParameterBound(typeParameterDescriptor, typeParameter, typeParameterListOwner)
        }
    }

    private fun checkConstructorInInterface(klass: KtClass) {
        klass.getPrimaryConstructor()?.let { trace.report(CONSTRUCTOR_IN_INTERFACE.on(it)) }
    }

    private fun checkMethodsOfAnyInInterface(classDescriptor: ClassDescriptorWithResolutionScopes) {
        for (declaredCallableMember in classDescriptor.declaredCallableMembers) {
            if (declaredCallableMember !is FunctionDescriptor) continue

            val declaration = DescriptorToSourceUtils.descriptorToDeclaration(declaredCallableMember)
            if (declaration !is KtNamedFunction) continue

            if (isHidingParentMemberIfPresent(declaredCallableMember)) continue

            if (isImplementingMethodOfAny(declaredCallableMember)) {
                trace.report(METHOD_OF_ANY_IMPLEMENTED_IN_INTERFACE.on(declaration))
            }
        }
    }

    private fun checkAnnotationClassWithBody(classOrObject: KtClassOrObject) {
        classOrObject.getBody()?.let { trace.report(ANNOTATION_CLASS_WITH_BODY.on(it)) }
    }

    private fun checkValOnAnnotationParameter(aClass: KtClass) {
        for (parameter in aClass.getPrimaryConstructorParameters()) {
            if (!parameter.hasValOrVar()) {
                trace.report(MISSING_VAL_ON_ANNOTATION_PARAMETER.on(parameter))
            }
        }
    }

    private fun checkOpenMembers(classDescriptor: ClassDescriptorWithResolutionScopes) {
        if (classCanHaveOpenMembers(classDescriptor)) return

        for (memberDescriptor in classDescriptor.declaredCallableMembers) {
            if (memberDescriptor.kind != CallableMemberDescriptor.Kind.DECLARATION) continue
            val member = DescriptorToSourceUtils.descriptorToDeclaration(memberDescriptor) as? KtNamedDeclaration
            if (member != null && member.hasModifier(KtTokens.OPEN_KEYWORD)) {
                if (classDescriptor.kind == ClassKind.OBJECT) {
                    trace.report(NON_FINAL_MEMBER_IN_OBJECT.on(member))
                }
                else {
                    trace.report(NON_FINAL_MEMBER_IN_FINAL_CLASS.on(member))
                }
            }
        }
    }

    private fun checkProperty(property: KtProperty, propertyDescriptor: PropertyDescriptor) {
        val containingDeclaration = propertyDescriptor.containingDeclaration
        if (containingDeclaration is ClassDescriptor) {
            checkPropertyAbstractness(property, propertyDescriptor, containingDeclaration)
        }
        checkPropertyLateInit(property, propertyDescriptor)
        checkPropertyInitializer(property, propertyDescriptor)
        checkAccessors(property, propertyDescriptor)
        checkTypeParameterConstraints(property)
        checkPropertyExposedType(property, propertyDescriptor)
        checkPropertyTypeParametersAreUsedInReceiverType(propertyDescriptor)
        checkImplicitCallableType(property, propertyDescriptor)
    }

    private fun checkPropertyTypeParametersAreUsedInReceiverType(descriptor: PropertyDescriptor) {
        val allTypeParameters = descriptor.typeParameters.toSet()
        val allAccessibleTypeParameters = HashSet<TypeParameterDescriptor>()

        fun addAccessibleTypeParametersFromType(type: KotlinType?) {
            TypeUtils.contains(type) {
                val declarationDescriptor = it.constructor.declarationDescriptor
                if (declarationDescriptor is TypeParameterDescriptor && declarationDescriptor in allTypeParameters) {
                    if (allAccessibleTypeParameters.add(declarationDescriptor)) {
                        declarationDescriptor.upperBounds.forEach {
                            addAccessibleTypeParametersFromType(it)
                        }
                    }
                }
                false
            }
        }
        addAccessibleTypeParametersFromType(descriptor.extensionReceiverParameter?.type)

        val typeParametersInaccessibleFromReceiver = allTypeParameters - allAccessibleTypeParameters
        for (typeParameter in typeParametersInaccessibleFromReceiver) {
            val typeParameterPsi = DescriptorToSourceUtils.getSourceFromDescriptor(typeParameter)
            if (typeParameterPsi is KtTypeParameter) {
                trace.report(TYPE_PARAMETER_OF_PROPERTY_NOT_USED_IN_RECEIVER.on(typeParameterPsi))
            }
        }
    }

    private fun checkPropertyLateInit(property: KtCallableDeclaration, propertyDescriptor: PropertyDescriptor) {
        val modifierList = property.modifierList ?: return
        val modifier = modifierList.getModifier(KtTokens.LATEINIT_KEYWORD) ?: return

        if (!propertyDescriptor.isVar) {
            trace.report(INAPPLICABLE_LATEINIT_MODIFIER.on(modifier, "is allowed only on mutable properties"))
        }

        var returnTypeIsNullable = true
        var returnTypeIsPrimitive = true

        val returnType = propertyDescriptor.returnType
        if (returnType != null) {
            returnTypeIsNullable = TypeUtils.isNullableType(returnType)
            returnTypeIsPrimitive = KotlinBuiltIns.isPrimitiveType(returnType)
        }

        if (returnTypeIsNullable) {
            trace.report(INAPPLICABLE_LATEINIT_MODIFIER.on(modifier, "is not allowed on nullable properties"))
        }

        if (returnTypeIsPrimitive) {
            trace.report(INAPPLICABLE_LATEINIT_MODIFIER.on(modifier, "is not allowed on primitive type properties"))
        }

        val isAbstract = propertyDescriptor.modality == Modality.ABSTRACT
        if (isAbstract) {
            trace.report(INAPPLICABLE_LATEINIT_MODIFIER.on(modifier, "is not allowed on abstract properties"))
        }

        if (property is KtParameter) {
            trace.report(INAPPLICABLE_LATEINIT_MODIFIER.on(modifier, "is not allowed on primary constructor parameters"))
        }

        var hasDelegateExpressionOrInitializer = false
        if (property is KtProperty && property.hasDelegateExpressionOrInitializer()) {
            hasDelegateExpressionOrInitializer = true
            trace.report(INAPPLICABLE_LATEINIT_MODIFIER.on(modifier,
                                                           "is not allowed on properties with initializer or on delegated properties"))
        }

        val hasAccessorImplementation = propertyDescriptor.hasAccessorImplementation()

        if (!hasDelegateExpressionOrInitializer && hasAccessorImplementation) {
            trace.report(INAPPLICABLE_LATEINIT_MODIFIER.on(modifier, "is not allowed on properties with a custom getter or setter"))
        }

        val hasBackingField = trace.bindingContext.get(BindingContext.BACKING_FIELD_REQUIRED, propertyDescriptor) ?: false

        if (!isAbstract && !hasAccessorImplementation && !hasDelegateExpressionOrInitializer && !hasBackingField) {
            trace.report(INAPPLICABLE_LATEINIT_MODIFIER.on(modifier, "is not allowed on properties without backing field"))
        }

        if (propertyDescriptor.extensionReceiverParameter != null) {
            trace.report(INAPPLICABLE_LATEINIT_MODIFIER.on(modifier, "is not allowed on extension properties"))
        }
    }

    private fun checkPropertyAbstractness(
            property: KtProperty,
            propertyDescriptor: PropertyDescriptor,
            classDescriptor: ClassDescriptor) {
        val modifierList = property.modifierList

        if (modifierList != null && modifierList.hasModifier(KtTokens.ABSTRACT_KEYWORD)) {
            //has abstract modifier
            if (!classCanHaveAbstractMembers(classDescriptor)) {
                trace.report(ABSTRACT_PROPERTY_IN_NON_ABSTRACT_CLASS.on(property, property.name ?: "", classDescriptor))
                return
            }
            if (classDescriptor.kind == ClassKind.INTERFACE) {
                trace.report(ABSTRACT_MODIFIER_IN_INTERFACE.on(property))
            }
        }

        if (propertyDescriptor.modality == Modality.ABSTRACT) {
            property.initializer?.let { trace.report(ABSTRACT_PROPERTY_WITH_INITIALIZER.on(it)) }
            property.delegate?.let { trace.report(ABSTRACT_DELEGATED_PROPERTY.on(it)) }
            val getter = property.getter
            if (getter != null && getter.hasBody()) {
                trace.report(ABSTRACT_PROPERTY_WITH_GETTER.on(getter))
            }
            val setter = property.setter
            if (setter != null && setter.hasBody()) {
                trace.report(ABSTRACT_PROPERTY_WITH_SETTER.on(setter))
            }
        }
    }

    private fun checkPropertyInitializer(property: KtProperty, propertyDescriptor: PropertyDescriptor) {
        val hasAccessorImplementation = propertyDescriptor.hasAccessorImplementation()

        val containingDeclaration = propertyDescriptor.containingDeclaration
        val inTrait = containingDeclaration is ClassDescriptor && containingDeclaration.kind == ClassKind.INTERFACE
        if (propertyDescriptor.modality == Modality.ABSTRACT) {
            if (!property.hasDelegateExpressionOrInitializer() && property.typeReference == null) {
                trace.report(PROPERTY_WITH_NO_TYPE_NO_INITIALIZER.on(property))
            }
            if (inTrait && property.hasModifier(KtTokens.PRIVATE_KEYWORD) && !property.hasModifier(KtTokens.ABSTRACT_KEYWORD)) {
                trace.report(PRIVATE_PROPERTY_IN_INTERFACE.on(property))
            }
            return
        }

        val backingFieldRequired = trace.bindingContext.get(BindingContext.BACKING_FIELD_REQUIRED, propertyDescriptor) ?: false
        if (inTrait && backingFieldRequired && hasAccessorImplementation) {
            trace.report(BACKING_FIELD_IN_INTERFACE.on(property))
        }

        val initializer = property.initializer
        val delegate = property.delegate
        if (initializer != null) {
            if (inTrait) {
                trace.report(PROPERTY_INITIALIZER_IN_INTERFACE.on(initializer))
            }
            else {
                if (!backingFieldRequired) {
                    trace.report(PROPERTY_INITIALIZER_NO_BACKING_FIELD.on(initializer))
                }
                else if (property.receiverTypeReference != null) {
                    trace.report(EXTENSION_PROPERTY_WITH_BACKING_FIELD.on(initializer))
                }
            }
        }
        else if (delegate != null) {
            if (inTrait) {
                trace.report(DELEGATED_PROPERTY_IN_INTERFACE.on(delegate))
            }
        }
        else {
            if (backingFieldRequired && !inTrait && !propertyDescriptor.isLateInit &&
                trace.bindingContext.get(BindingContext.IS_UNINITIALIZED, propertyDescriptor) ?: false) {
                if (containingDeclaration !is ClassDescriptor || hasAccessorImplementation) {
                    trace.report(MUST_BE_INITIALIZED.on(property))
                }
                else {
                    trace.report(MUST_BE_INITIALIZED_OR_BE_ABSTRACT.on(property))
                }
            }
            else if (property.typeReference == null) {
                trace.report(PROPERTY_WITH_NO_TYPE_NO_INITIALIZER.on(property))
            }
        }
    }

    private fun checkMemberReceiverExposedType(typeReference: KtTypeReference?, memberDescriptor: CallableMemberDescriptor) {
        if (typeReference == null) return
        val receiverParameterDescriptor = memberDescriptor.extensionReceiverParameter ?: return
        val memberVisibility = memberDescriptor.effectiveVisibility()
        val receiverTypeVisibility = receiverParameterDescriptor.type.effectiveVisibility()
        if (!receiverTypeVisibility.sameOrMorePermissive(memberVisibility)) {
            trace.report(EXPOSED_RECEIVER_TYPE.on(typeReference, memberVisibility, receiverTypeVisibility))
        }
    }

    private fun checkPropertyExposedType(property: KtProperty, propertyDescriptor: PropertyDescriptor) {
        val propertyVisibility = propertyDescriptor.effectiveVisibility()
        val typeVisibility = propertyDescriptor.type.effectiveVisibility()
        if (!typeVisibility.sameOrMorePermissive(propertyVisibility)) {
            trace.report(EXPOSED_PROPERTY_TYPE.on(property.nameIdentifier ?: property, propertyVisibility, typeVisibility))
        }
        checkMemberReceiverExposedType(property.receiverTypeReference, propertyDescriptor)
    }

    fun checkFunction(function: KtNamedFunction, functionDescriptor: SimpleFunctionDescriptor) {
        val typeParameterList = function.typeParameterList
        val nameIdentifier = function.nameIdentifier
        if (typeParameterList != null && nameIdentifier != null &&
            typeParameterList.textRange.startOffset > nameIdentifier.textRange.startOffset) {
            trace.report(DEPRECATED_TYPE_PARAMETER_SYNTAX.on(typeParameterList))
        }
        checkTypeParameterConstraints(function)

        val containingDescriptor = functionDescriptor.containingDeclaration
        val hasAbstractModifier = function.hasModifier(KtTokens.ABSTRACT_KEYWORD)
        val hasExternalModifier = function.hasModifier(KtTokens.EXTERNAL_KEYWORD)

        if (containingDescriptor is ClassDescriptor) {
            val inTrait = containingDescriptor.kind == ClassKind.INTERFACE
            if (hasAbstractModifier && !classCanHaveAbstractMembers(containingDescriptor)) {
                trace.report(ABSTRACT_FUNCTION_IN_NON_ABSTRACT_CLASS.on(function, functionDescriptor.name.asString(), containingDescriptor))
            }
            if (hasAbstractModifier && inTrait) {
                trace.report(ABSTRACT_MODIFIER_IN_INTERFACE.on(function))
            }
            val hasBody = function.hasBody()
            if (hasBody && hasAbstractModifier) {
                trace.report(ABSTRACT_FUNCTION_WITH_BODY.on(function, functionDescriptor))
            }
            if (!hasBody && inTrait) {
                if (function.hasModifier(KtTokens.PRIVATE_KEYWORD)) {
                    trace.report(PRIVATE_FUNCTION_WITH_NO_BODY.on(function, functionDescriptor))
                }
            }
            if (!hasBody && !hasAbstractModifier && !hasExternalModifier && !inTrait) {
                trace.report(NON_ABSTRACT_FUNCTION_WITH_NO_BODY.on(function, functionDescriptor))
            }
            return
        }
        if (!function.hasBody() && !hasAbstractModifier && !hasExternalModifier) {
            trace.report(NON_MEMBER_FUNCTION_NO_BODY.on(function, functionDescriptor))
        }
        checkImplicitCallableType(function, functionDescriptor)
        checkFunctionExposedType(function, functionDescriptor)
        checkVarargParameters(trace, functionDescriptor)
    }

    private fun checkImplicitCallableType(declaration: KtCallableDeclaration, descriptor: CallableDescriptor) {
        descriptor.returnType?.let {
            if (declaration.typeReference == null) {
                val target = declaration.nameIdentifier ?: declaration
                if (it.isNothing()) {
                    trace.report(
                            (if (declaration is KtProperty) IMPLICIT_NOTHING_PROPERTY_TYPE else IMPLICIT_NOTHING_RETURN_TYPE).on(target)
                    )
                }
                if (it.constructor is IntersectionTypeConstructor) {
                    trace.report(IMPLICIT_INTERSECTION_TYPE.on(target, it))
                }
            }
        }
    }

    private fun checkFunctionExposedType(function: KtFunction, functionDescriptor: FunctionDescriptor) {
        val functionVisibility = functionDescriptor.effectiveVisibility()
        if (function !is KtConstructor<*>) {
            val returnTypeVisibility = functionDescriptor.returnType?.effectiveVisibility()
            if (returnTypeVisibility != null && !returnTypeVisibility.sameOrMorePermissive(functionVisibility)) {
                trace.report(EXPOSED_FUNCTION_RETURN_TYPE.on(function.nameIdentifier ?: function, functionVisibility, returnTypeVisibility))
            }
        }
        functionDescriptor.valueParameters.forEachIndexed { i, parameterDescriptor ->
            val typeVisibility = parameterDescriptor.type.effectiveVisibility()
            if (!typeVisibility.sameOrMorePermissive(functionVisibility) && i < function.valueParameters.size) {
                trace.report(EXPOSED_PARAMETER_TYPE.on(function.valueParameters[i], functionVisibility, typeVisibility))
            }
        }
        checkMemberReceiverExposedType(function.receiverTypeReference, functionDescriptor)
    }

    private fun checkAccessors(property: KtProperty, propertyDescriptor: PropertyDescriptor) {
        for (accessor in property.accessors) {
            val propertyAccessorDescriptor = (if (accessor.isGetter) propertyDescriptor.getter else propertyDescriptor.setter)
                                             ?: throw AssertionError("No property accessor descriptor for ${property.text}")
            accessor.checkTypeReferences()
            modifiersChecker.checkModifiersForDeclaration(accessor, propertyAccessorDescriptor)
            identifierChecker.checkDeclaration(accessor, trace)
        }
        checkAccessor(propertyDescriptor, property.getter, propertyDescriptor.getter)
        checkAccessor(propertyDescriptor, property.setter, propertyDescriptor.setter)
    }

    private fun reportVisibilityModifierDiagnostics(tokens: Collection<PsiElement>, diagnostic: DiagnosticFactory0<PsiElement>) {
        for (token in tokens) {
            trace.report(diagnostic.on(token))
        }
    }

    private fun checkAccessor(
            propertyDescriptor: PropertyDescriptor,
            accessor: KtPropertyAccessor?,
            accessorDescriptor: PropertyAccessorDescriptor?) {
        if (accessor == null || accessorDescriptor == null) return
        val accessorModifierList = accessor.modifierList ?: return
        val tokens = modifiersChecker.getTokensCorrespondingToModifiers(accessorModifierList,
                                                                        Sets.newHashSet(KtTokens.PUBLIC_KEYWORD, KtTokens.PROTECTED_KEYWORD, KtTokens.PRIVATE_KEYWORD, KtTokens.INTERNAL_KEYWORD))
        if (accessor.isGetter) {
            if (accessorDescriptor.visibility != propertyDescriptor.visibility) {
                reportVisibilityModifierDiagnostics(tokens.values, Errors.GETTER_VISIBILITY_DIFFERS_FROM_PROPERTY_VISIBILITY)
            }
            else {
                reportVisibilityModifierDiagnostics(tokens.values, Errors.REDUNDANT_MODIFIER_IN_GETTER)
            }
        }
        else {
            if (propertyDescriptor.isOverridable
                && accessorDescriptor.visibility == Visibilities.PRIVATE
                && propertyDescriptor.visibility != Visibilities.PRIVATE) {
                if (propertyDescriptor.modality == Modality.ABSTRACT) {
                    reportVisibilityModifierDiagnostics(tokens.values, Errors.PRIVATE_SETTER_FOR_ABSTRACT_PROPERTY)
                }
                else {
                    reportVisibilityModifierDiagnostics(tokens.values, Errors.PRIVATE_SETTER_FOR_OPEN_PROPERTY)
                }
            }
            else {
                val compare = Visibilities.compare(accessorDescriptor.visibility, propertyDescriptor.visibility)
                if (compare == null || compare > 0) {
                    reportVisibilityModifierDiagnostics(tokens.values, Errors.SETTER_VISIBILITY_INCONSISTENT_WITH_PROPERTY_VISIBILITY)
                }
            }
        }
    }

    private fun checkEnumEntry(enumEntry: KtEnumEntry, classDescriptor: ClassDescriptor) {
        val declaration = classDescriptor.containingDeclaration
        if (DescriptorUtils.isEnumClass(declaration)) {
            if (!enumEntry.hasInitializer() && !hasDefaultConstructor(declaration as ClassDescriptor)) {
                trace.report(ENUM_ENTRY_SHOULD_BE_INITIALIZED.on(enumEntry))
            }
        }
        else {
            assert(DescriptorUtils.isInterface(declaration)) { "Enum entry should be declared in enum class: " + classDescriptor }
        }
    }

    private fun checkVarargParameters(trace: BindingTrace, callableDescriptor: CallableDescriptor) {
        val numberOfVarargParameters = callableDescriptor.valueParameters.count { it.varargElementType != null }
        if (numberOfVarargParameters > 1) {
            for (parameter in callableDescriptor.valueParameters) {
                if (parameter.varargElementType != null) {
                    val parameterDeclaration = DescriptorToSourceUtils.descriptorToDeclaration(parameter)
                    if (parameterDeclaration is KtParameter) {
                        trace.report(MULTIPLE_VARARG_PARAMETERS.on(parameterDeclaration))
                    }
                }
            }
        }
    }

    companion object {

        private fun removeDuplicateTypes(conflictingTypes: MutableSet<KotlinType>) {
            val iterator = conflictingTypes.iterator()
            while (iterator.hasNext()) {
                val type = iterator.next()
                for (otherType in conflictingTypes) {
                    val subtypeOf = KotlinTypeChecker.DEFAULT.equalTypes(type, otherType)
                    if (type !== otherType && subtypeOf) {
                        iterator.remove()
                        break
                    }
                }
            }
        }

        private fun hasConstraints(typeParameter: KtTypeParameter, constraints: List<KtTypeConstraint>): Boolean {
            if (typeParameter.name == null) return false
            return constraints.any { it.subjectTypeParameterName?.text == typeParameter.name }
        }

        private val METHOD_OF_ANY_NAMES = ImmutableSet.of("toString", "hashCode", "equals")

        private fun isImplementingMethodOfAny(member: CallableMemberDescriptor): Boolean {
            if (!METHOD_OF_ANY_NAMES.contains(member.name.asString())) return false
            if (member.modality == Modality.ABSTRACT) return false

            return isImplementingMethodOfAnyInternal(member, HashSet<ClassDescriptor>())
        }

        private fun isImplementingMethodOfAnyInternal(member: CallableMemberDescriptor, visitedClasses: MutableSet<ClassDescriptor>): Boolean {
            for (overridden in member.overriddenDescriptors) {
                val containingDeclaration = overridden.containingDeclaration
                if (containingDeclaration !is ClassDescriptor) continue
                if (visitedClasses.contains(containingDeclaration)) continue

                if (DescriptorUtils.getFqName(containingDeclaration) == KotlinBuiltIns.FQ_NAMES.any) {
                    return true
                }

                if (isHidingParentMemberIfPresent(overridden)) continue

                visitedClasses.add(containingDeclaration)

                if (isImplementingMethodOfAnyInternal(overridden, visitedClasses)) {
                    return true
                }
            }

            return false
        }

        private fun isHidingParentMemberIfPresent(member: CallableMemberDescriptor): Boolean {
            val declaration = DescriptorToSourceUtils.descriptorToDeclaration(member) as? KtNamedDeclaration ?: return false
            val modifierList = declaration.modifierList ?: return true
            return !modifierList.hasModifier(KtTokens.OVERRIDE_KEYWORD)
        }

        private fun hasDefaultConstructor(classDescriptor: ClassDescriptor) =
                classDescriptor.constructors.any { it.valueParameters.isEmpty() }

        private fun PropertyDescriptor.hasAccessorImplementation(): Boolean {
            getter?.let { if (it.hasBody()) return true }
            setter?.let { if (it.hasBody()) return true }
            return false
        }
    }
}
