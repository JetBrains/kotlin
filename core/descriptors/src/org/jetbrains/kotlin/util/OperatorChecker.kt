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

package org.jetbrains.kotlin.util

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.ReflectionTypes
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.descriptorUtil.hasDefaultValue
import org.jetbrains.kotlin.resolve.descriptorUtil.isExtension
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.types.typeUtil.*
import org.jetbrains.kotlin.util.OperatorNameConventions.GET
import org.jetbrains.kotlin.util.OperatorNameConventions.SET
import org.jetbrains.kotlin.util.OperatorNameConventions.INVOKE
import org.jetbrains.kotlin.util.OperatorNameConventions.CONTAINS
import org.jetbrains.kotlin.util.OperatorNameConventions.ITERATOR
import org.jetbrains.kotlin.util.OperatorNameConventions.NEXT
import org.jetbrains.kotlin.util.OperatorNameConventions.HAS_NEXT
import org.jetbrains.kotlin.util.OperatorNameConventions.EQUALS
import org.jetbrains.kotlin.util.OperatorNameConventions.COMPARE_TO
import org.jetbrains.kotlin.util.OperatorNameConventions.BINARY_OPERATION_NAMES
import org.jetbrains.kotlin.util.OperatorNameConventions.ASSIGNMENT_OPERATIONS
import org.jetbrains.kotlin.util.OperatorNameConventions.COMPONENT_REGEX
import org.jetbrains.kotlin.util.OperatorNameConventions.PLUS
import org.jetbrains.kotlin.util.OperatorNameConventions.MINUS
import org.jetbrains.kotlin.util.OperatorNameConventions.RANGE_TO
import org.jetbrains.kotlin.util.OperatorNameConventions.UNARY_PLUS
import org.jetbrains.kotlin.util.OperatorNameConventions.UNARY_MINUS
import org.jetbrains.kotlin.util.OperatorNameConventions.NOT
import org.jetbrains.kotlin.util.OperatorNameConventions.INC
import org.jetbrains.kotlin.util.OperatorNameConventions.DEC
import org.jetbrains.kotlin.util.OperatorNameConventions.GET_VALUE
import org.jetbrains.kotlin.util.OperatorNameConventions.SET_VALUE

object OperatorChecks {
    fun canBeOperator(functionDescriptor: FunctionDescriptor): Boolean {
        val name = functionDescriptor.name

        return with (functionDescriptor) {
            if (!functionDescriptor.isMemberOrExtension) return false

            when {
                GET == name -> valueParameters.size >= 1
                SET == name -> {
                    val lastIsOk = valueParameters.lastOrNull()?.let { !it.hasDefaultValue() && it.varargElementType == null } ?: false
                    valueParameters.size >= 2 && lastIsOk
                }
                
                GET_VALUE == name -> noDefaultsAndVarargs && valueParameters.size >= 2 && valueParameters[1].isKProperty
                SET_VALUE == name -> noDefaultsAndVarargs && valueParameters.size >= 3 && valueParameters[1].isKProperty
                
                INVOKE == name -> isMemberOrExtension
                CONTAINS == name -> singleValueParameter && noDefaultsAndVarargs && returnsBoolean
                
                ITERATOR == name -> noValueParameters
                NEXT == name -> noValueParameters
                HAS_NEXT == name -> noValueParameters && returnsBoolean
                
                RANGE_TO == name -> singleValueParameter && noDefaultsAndVarargs
                
                EQUALS == name -> {
                    fun DeclarationDescriptor.isAny() = (this as? ClassDescriptor)?.let { KotlinBuiltIns.isAny(it) } ?: false
                    isMember && overriddenDescriptors.any { it.containingDeclaration.isAny() }
                }
                COMPARE_TO == name -> returnsInt && singleValueParameter && noDefaultsAndVarargs
                
                BINARY_OPERATION_NAMES.any { it == name } && functionDescriptor.valueParameters.size == 1 -> 
                    singleValueParameter && noDefaultsAndVarargs
                (PLUS == name) || (MINUS == name) || (UNARY_PLUS == name) || (UNARY_MINUS == name) || (NOT == name) ->
                    noValueParameters
                (INC == name) || (DEC == name) -> {
                    val receiver = dispatchReceiverParameter ?: extensionReceiverParameter
                    isMemberOrExtension && (receiver != null) && (returnType?.let { it.isSubtypeOf(receiver.type) } ?: false)
                }
                
                ASSIGNMENT_OPERATIONS.any { it == name } ->
                    returnsUnit && singleValueParameter && noDefaultsAndVarargs
                
                name.asString().matches(COMPONENT_REGEX) -> noValueParameters
                else -> false
            }
        }
    }

    private val ValueParameterDescriptor.isKProperty: Boolean
        get() = ReflectionTypes.createKPropertyStarType(module)?.isSubtypeOf(type.makeNotNullable()) ?: false

    private val FunctionDescriptor.isMember: Boolean
        get() = containingDeclaration is ClassDescriptor

    private val FunctionDescriptor.isMemberOrExtension: Boolean
        get() = isExtension || containingDeclaration is ClassDescriptor

    private val FunctionDescriptor.noValueParameters: Boolean
        get() = valueParameters.isEmpty()

    private val FunctionDescriptor.singleValueParameter: Boolean
        get() = valueParameters.size == 1

    private val FunctionDescriptor.returnsBoolean: Boolean
        get() = returnType?.let { KotlinBuiltIns.isBoolean(it) } ?: false

    private val FunctionDescriptor.returnsInt: Boolean
        get() = returnType?.let { builtIns.intType == it } ?: false //TODO

    private val FunctionDescriptor.returnsUnit: Boolean
        get() = returnType?.let { builtIns.unitType == it } ?: false //TODO

    private val FunctionDescriptor.noDefaultsAndVarargs: Boolean
        get() = valueParameters.all { !it.hasDefaultValue() && it.varargElementType == null }

}