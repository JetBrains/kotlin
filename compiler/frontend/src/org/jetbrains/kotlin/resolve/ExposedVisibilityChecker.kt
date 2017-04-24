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

package org.jetbrains.kotlin.resolve

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.diagnostics.DiagnosticSink.DO_NOTHING
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.isError

// Checker for all seven EXPOSED_* errors
// All functions return true if everything is OK, or false in case of any errors
class ExposedVisibilityChecker(private val trace: DiagnosticSink = DO_NOTHING) {

    // NB: does not check any members
    fun checkClassHeader(klass: KtClassOrObject, classDescriptor: ClassDescriptor): Boolean {
        var result = checkSupertypes(klass, classDescriptor)
        result = result and checkParameterBounds(klass, classDescriptor)

        val constructor = klass.primaryConstructor ?: return result
        val constructorDescriptor = classDescriptor.unsubstitutedPrimaryConstructor ?: return result
        return result and checkFunction(constructor, constructorDescriptor)
    }

    fun checkDeclarationWithVisibility(modifierListOwner: KtModifierListOwner,
                                       descriptor: DeclarationDescriptorWithVisibility,
                                       visibility: Visibility
    ) : Boolean {
        return when {
            modifierListOwner is KtFunction &&
            descriptor is FunctionDescriptor -> checkFunction(modifierListOwner, descriptor, visibility)

            modifierListOwner is KtProperty &&
            descriptor is PropertyDescriptor -> checkProperty(modifierListOwner, descriptor, visibility)

            else -> true
        }
    }

    fun checkTypeAlias(typeAlias: KtTypeAlias, typeAliasDescriptor: TypeAliasDescriptor) {
        val expandedType = typeAliasDescriptor.expandedType
        if (expandedType.isError) return

        val typeAliasVisibility = typeAliasDescriptor.effectiveVisibility()
        val restricting = expandedType.leastPermissiveDescriptor(typeAliasVisibility)
        if (restricting != null) {
            trace.report(Errors.EXPOSED_TYPEALIAS_EXPANDED_TYPE.on(typeAlias.nameIdentifier ?: typeAlias,
                    typeAliasVisibility, restricting, restricting.effectiveVisibility()))
        }
    }

    fun checkFunction(function: KtFunction,
                      functionDescriptor: FunctionDescriptor,
                      // for checking situation with modified basic visibility
                      visibility: Visibility = functionDescriptor.visibility
    ): Boolean {
        val functionVisibility = functionDescriptor.effectiveVisibility(visibility)
        var result = true
        if (function !is KtConstructor<*>) {
            val restricting = functionDescriptor.returnType?.leastPermissiveDescriptor(functionVisibility)
            if (restricting != null) {
                trace.report(Errors.EXPOSED_FUNCTION_RETURN_TYPE.on(function.nameIdentifier ?: function, functionVisibility,
                                                                    restricting, restricting.effectiveVisibility()))
                result = false
            }
        }
        functionDescriptor.valueParameters.forEachIndexed { i, parameterDescriptor ->
            val restricting = parameterDescriptor.type.leastPermissiveDescriptor(functionVisibility)
            if (restricting != null && i < function.valueParameters.size) {
                trace.report(Errors.EXPOSED_PARAMETER_TYPE.on(function.valueParameters[i], functionVisibility,
                                                              restricting, restricting.effectiveVisibility()))
                result = false
            }
        }
        return result and checkMemberReceiver(function.receiverTypeReference, functionDescriptor)
    }

    fun checkProperty(property: KtProperty,
                      propertyDescriptor: PropertyDescriptor,
                      // for checking situation with modified basic visibility
                      visibility: Visibility = propertyDescriptor.visibility
    ): Boolean {
        val propertyVisibility = propertyDescriptor.effectiveVisibility(visibility)
        val restricting = propertyDescriptor.type.leastPermissiveDescriptor(propertyVisibility)
        var result = true
        if (restricting != null) {
            trace.report(Errors.EXPOSED_PROPERTY_TYPE.on(property.nameIdentifier ?: property, propertyVisibility,
                                                         restricting, restricting.effectiveVisibility()))
            result = false
        }
        return result and checkMemberReceiver(property.receiverTypeReference, propertyDescriptor)
    }

    private fun checkMemberReceiver(typeReference: KtTypeReference?, memberDescriptor: CallableMemberDescriptor): Boolean {
        if (typeReference == null) return true
        val receiverParameterDescriptor = memberDescriptor.extensionReceiverParameter ?: return true
        val memberVisibility = memberDescriptor.effectiveVisibility()
        val restricting = receiverParameterDescriptor.type.leastPermissiveDescriptor(memberVisibility)
        if (restricting != null) {
            trace.report(Errors.EXPOSED_RECEIVER_TYPE.on(typeReference, memberVisibility,
                                                         restricting, restricting.effectiveVisibility()))
            return false
        }
        return true
    }

    private fun checkSupertypes(klass: KtClassOrObject, classDescriptor: ClassDescriptor): Boolean {
        val classVisibility = classDescriptor.effectiveVisibility()
        val isInterface = classDescriptor.kind == ClassKind.INTERFACE
        val delegationList = klass.superTypeListEntries
        var result = true
        classDescriptor.typeConstructor.supertypes.forEachIndexed { i, superType ->
            if (i >= delegationList.size) return result
            val superDescriptor = TypeUtils.getClassDescriptor(superType) ?: return@forEachIndexed
            val superIsInterface = superDescriptor.kind == ClassKind.INTERFACE
            if (superIsInterface != isInterface) {
                return@forEachIndexed
            }
            val restricting = superType.leastPermissiveDescriptor(classVisibility)
            if (restricting != null) {
                if (isInterface) {
                    trace.report(Errors.EXPOSED_SUPER_INTERFACE.on(delegationList[i], classVisibility,
                                                                   restricting, restricting.effectiveVisibility()))
                }
                else {
                    trace.report(Errors.EXPOSED_SUPER_CLASS.on(delegationList[i], classVisibility,
                                                               restricting, restricting.effectiveVisibility()))
                }
                result = false
            }
        }
        return result
    }

    private fun checkParameterBounds(klass: KtClassOrObject, classDescriptor: ClassDescriptor): Boolean {
        val classVisibility = classDescriptor.effectiveVisibility()
        val typeParameterList = klass.typeParameters
        var result = true
        classDescriptor.declaredTypeParameters.forEachIndexed { i, typeParameterDescriptor ->
            if (i >= typeParameterList.size) return result
            for (upperBound in typeParameterDescriptor.upperBounds) {
                val restricting = upperBound.leastPermissiveDescriptor(classVisibility)
                if (restricting != null) {
                    trace.report(Errors.EXPOSED_TYPE_PARAMETER_BOUND.on(typeParameterList[i], classVisibility,
                                                                        restricting, restricting.effectiveVisibility()))
                    result = false
                    break
                }
            }
        }
        return result
    }
}

