/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.load.kotlin.sam

import org.jetbrains.kotlin.builtins.createFunctionType
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations.Companion.EMPTY
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.SamConversionResolver
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.SimpleType
import java.util.*

class SamConversionResolverImpl(
        storageManager: StorageManager,
        private val samWithReceiverResolvers: Iterable<SamWithReceiverResolver>
): SamConversionResolver {
    private val functionTypesForSamInterfaces = storageManager.createCacheWithNullableValues<ClassDescriptor, SimpleType>()

    override fun resolveFunctionTypeIfSamInterface(classDescriptor: ClassDescriptor): SimpleType? {
        return functionTypesForSamInterfaces.computeIfAbsent(classDescriptor) {
            val abstractMethod = getSingleAbstractMethodOrNull(classDescriptor) ?: return@computeIfAbsent null
            val shouldConvertFirstParameterToDescriptor = samWithReceiverResolvers.any { it.shouldConvertFirstSamParameterToReceiver(abstractMethod) }
            getFunctionTypeForAbstractMethod(abstractMethod, shouldConvertFirstParameterToDescriptor)
        }
    }
}

fun getSingleAbstractMethodOrNull(klass: ClassDescriptor): FunctionDescriptor? {
    // NB: this check MUST BE at start. Please do not touch until following to-do is resolved
    // Otherwise android data binding can cause resolve re-entrance
    // For details see KT-18687, KT-16149
    // TODO: prevent resolve re-entrance on architecture level, or (alternatively) ask data binding owners not to do it
    if (klass.fqNameSafe.asString().endsWith(".databinding.DataBindingComponent")) return null

    if (klass.isDefinitelyNotSamInterface) return null

    val abstractMember = getAbstractMembers(klass).singleOrNull() ?: return null

    return if (abstractMember is SimpleFunctionDescriptor && abstractMember.typeParameters.isEmpty())
        abstractMember
    else
        null
}

@Suppress("UNCHECKED_CAST")
fun getAbstractMembers(classDescriptor: ClassDescriptor): List<CallableMemberDescriptor> {
    return DescriptorUtils
        .getAllDescriptors(classDescriptor.unsubstitutedMemberScope)
        .filter { it is CallableMemberDescriptor && it.modality == Modality.ABSTRACT } as List<CallableMemberDescriptor>
}

fun getFunctionTypeForAbstractMethod(
    function: FunctionDescriptor,
    shouldConvertFirstParameterToDescriptor: Boolean
): SimpleType {
    val returnType = function.returnType ?: error("function is not initialized: $function")
    val valueParameters = function.valueParameters

    val parameterTypes = ArrayList<KotlinType>(valueParameters.size)
    val parameterNames = ArrayList<Name>(valueParameters.size)

    var startIndex = 0
    var receiverType: KotlinType? = null
    if (shouldConvertFirstParameterToDescriptor && function.valueParameters.isNotEmpty()) {
        receiverType = valueParameters[0].type
        startIndex = 1
    }

    for (i in startIndex until valueParameters.size) {
        val parameter = valueParameters[i]
        parameterTypes.add(parameter.type)
        parameterNames.add(if (function.hasSynthesizedParameterNames()) SpecialNames.NO_NAME_PROVIDED else parameter.name)
    }

    return createFunctionType(
        function.builtIns, EMPTY, receiverType, parameterTypes,
        parameterNames, returnType, function.isSuspend
    )
}