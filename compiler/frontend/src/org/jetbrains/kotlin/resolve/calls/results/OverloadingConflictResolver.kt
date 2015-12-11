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

package org.jetbrains.kotlin.resolve.calls.results

import gnu.trove.THashSet
import gnu.trove.TObjectHashingStrategy
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ScriptDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.resolve.OverrideResolver
import org.jetbrains.kotlin.resolve.calls.context.CheckArgumentTypesMode
import org.jetbrains.kotlin.resolve.calls.model.MutableResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.VariableAsFunctionResolvedCall
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker

class OverloadingConflictResolver(private val builtIns: KotlinBuiltIns) {

    fun <D : CallableDescriptor> findMaximallySpecific(
            candidates: Set<MutableResolvedCall<D>>,
            discriminateGenericDescriptors: Boolean,
            checkArgumentsMode: CheckArgumentTypesMode
    ): MutableResolvedCall<D>? {
        // Different smartcasts may lead to the same candidate descriptor wrapped into different ResolvedCallImpl objects
        
        val maximallySpecific = THashSet(object : TObjectHashingStrategy<MutableResolvedCall<D>> {
            override fun equals(call1: MutableResolvedCall<D>?, call2: MutableResolvedCall<D>?): Boolean =
                    if (call1 == null) call2 == null
                    else call1.resultingDescriptor == call2!!.resultingDescriptor

            override fun computeHashCode(call: MutableResolvedCall<D>?): Int =
                    call?.resultingDescriptor?.hashCode() ?: 0
        })
        
        candidates.filterTo(maximallySpecific) {
            isMaximallySpecific(it, candidates, discriminateGenericDescriptors, checkArgumentsMode)
        }

        return if (maximallySpecific.size == 1) maximallySpecific.first() else null
    }

    private fun <D : CallableDescriptor> isMaximallySpecific(
            candidateCall: MutableResolvedCall<D>,
            candidates: Set<MutableResolvedCall<D>>,
            discriminateGenericDescriptors: Boolean,
            checkArgumentsMode: CheckArgumentTypesMode): Boolean {
        val me = candidateCall.resultingDescriptor

        val isInvoke = candidateCall is VariableAsFunctionResolvedCall
        val variable = (candidateCall as? VariableAsFunctionResolvedCall)?.variableCall?.resultingDescriptor

        for (otherCall in candidates) {
            val other = otherCall.resultingDescriptor
            if (other === me) continue

            if (definitelyNotMaximallySpecific(me, other, discriminateGenericDescriptors, checkArgumentsMode)) {
                if (!isInvoke) return false

                assert(otherCall is VariableAsFunctionResolvedCall) { "'invoke' candidate goes with usual one: " + candidateCall + otherCall }

                val otherVariableCall = (otherCall as VariableAsFunctionResolvedCall).variableCall
                if (definitelyNotMaximallySpecific(variable!!, otherVariableCall.resultingDescriptor, discriminateGenericDescriptors, checkArgumentsMode)) {
                    return false
                }
            }
        }

        return true
    }

    private fun <D : CallableDescriptor> definitelyNotMaximallySpecific(
            me: D,
            other: D,
            discriminateGenericDescriptors: Boolean,
            checkArgumentsMode: CheckArgumentTypesMode): Boolean {
        return !moreSpecific(me, other, discriminateGenericDescriptors, checkArgumentsMode) ||
               moreSpecific(other, me, discriminateGenericDescriptors, checkArgumentsMode)
    }

    /**
     * Let < mean "more specific"
     * Subtype < supertype
     * Double < Float
     * Int < Long
     * Int < Short < Byte
     */
    private fun <Descriptor : CallableDescriptor> moreSpecific(
            f: Descriptor,
            g: Descriptor,
            discriminateGenericDescriptors: Boolean,
            checkArgumentsMode: CheckArgumentTypesMode): Boolean {
        val resolvingCallableReference = checkArgumentsMode == CheckArgumentTypesMode.CHECK_CALLABLE_TYPE

        if (f.containingDeclaration is ScriptDescriptor && g.containingDeclaration is ScriptDescriptor) {
            val fs = f.containingDeclaration as ScriptDescriptor
            val gs = g.containingDeclaration as ScriptDescriptor

            if (fs.priority != gs.priority) {
                return fs.priority > gs.priority
            }
        }

        val isGenericF = isGeneric(f)
        val isGenericG = isGeneric(g)
        if (discriminateGenericDescriptors) {
            if (!isGenericF && isGenericG) return true
            if (isGenericF && !isGenericG) return false

            if (isGenericF && isGenericG) {
                return moreSpecific(BoundsSubstitutor.substituteBounds(f),
                                    BoundsSubstitutor.substituteBounds(g),
                                    false, checkArgumentsMode)
            }
        }

        if (OverrideResolver.overrides(f, g)) return true
        if (OverrideResolver.overrides(g, f)) return false

        val receiverOfF = f.extensionReceiverParameter
        val receiverOfG = g.extensionReceiverParameter
        if (receiverOfF != null && receiverOfG != null) {
            if (!typeMoreSpecific(receiverOfF.type, receiverOfG.type)) return false
        }

        val fParams = f.valueParameters
        val gParams = g.valueParameters

        val fSize = fParams.size
        val gSize = gParams.size

        val fIsVararg = isVariableArity(fParams)
        val gIsVararg = isVariableArity(gParams)

        if (resolvingCallableReference && fIsVararg != gIsVararg) return false
        if (!fIsVararg && gIsVararg) return true
        if (fIsVararg && !gIsVararg) return false

        if (!fIsVararg && !gIsVararg) {
            if (resolvingCallableReference && fSize != gSize) return false
            if (!resolvingCallableReference && fSize > gSize) return false

            for (i in 0..fSize - 1) {
                val fParam = fParams[i]
                val gParam = gParams[i]

                val fParamType = fParam.type
                val gParamType = gParam.type

                if (!typeMoreSpecific(fParamType, gParamType)) {
                    return false
                }
            }
        }

        if (fIsVararg && gIsVararg) {
            // Check matching parameters
            val minSize = Math.min(fSize, gSize)
            for (i in 0..minSize - 1 - 1) {
                val fParam = fParams[i]
                val gParam = gParams[i]

                val fParamType = fParam.type
                val gParamType = gParam.type

                if (!typeMoreSpecific(fParamType, gParamType)) {
                    return false
                }
            }

            // Check the non-matching parameters of one function against the vararg parameter of the other function
            // Example:
            //   f(vararg vf : T)
            //   g(a : A, vararg vg : T)
            // here we check that typeOf(a) < elementTypeOf(vf) and elementTypeOf(vg) < elementTypeOf(vf)
            if (fSize < gSize) {
                val fParam = fParams[fSize - 1]
                val fParamType = fParam.varargElementType
                assert(fParamType != null) { "fIsVararg guarantees this" }
                for (i in fSize - 1..gSize - 1) {
                    val gParam = gParams[i]
                    if (!typeMoreSpecific(fParamType!!, getVarargElementTypeOrType(gParam))) {
                        return false
                    }
                }
            }
            else {
                val gParam = gParams[gSize - 1]
                val gParamType = gParam.varargElementType
                assert(gParamType != null) { "gIsVararg guarantees this" }
                for (i in gSize - 1..fSize - 1) {
                    val fParam = fParams[i]
                    if (!typeMoreSpecific(getVarargElementTypeOrType(fParam), gParamType!!)) {
                        return false
                    }
                }
            }
        }

        return true
    }

    private fun getVarargElementTypeOrType(parameterDescriptor: ValueParameterDescriptor): KotlinType =
            parameterDescriptor.varargElementType ?: parameterDescriptor.type

    private fun isVariableArity(fParams: List<ValueParameterDescriptor>): Boolean =
            fParams.lastOrNull()?.varargElementType != null

    private fun isGeneric(f: CallableDescriptor): Boolean =
            f.original.typeParameters.isNotEmpty()

    private fun typeMoreSpecific(specific: KotlinType, general: KotlinType): Boolean {
        val isSubtype = KotlinTypeChecker.DEFAULT.isSubtypeOf(specific, general) || numericTypeMoreSpecific(specific, general)

        if (!isSubtype) return false

        val sThanG = specific.getSpecificityRelationTo(general)
        val gThanS = general.getSpecificityRelationTo(specific)
        if (sThanG === Specificity.Relation.LESS_SPECIFIC &&
            gThanS !== Specificity.Relation.LESS_SPECIFIC) {
            return false
        }

        return true
    }

    private fun numericTypeMoreSpecific(specific: KotlinType, general: KotlinType): Boolean {
        val _double = builtIns.doubleType
        val _float = builtIns.floatType
        val _long = builtIns.longType
        val _int = builtIns.intType
        val _byte = builtIns.byteType
        val _short = builtIns.shortType

        when {
            TypeUtils.equalTypes(specific, _double) && TypeUtils.equalTypes(general, _float) -> return true
            TypeUtils.equalTypes(specific, _int) -> {
                when {
                    TypeUtils.equalTypes(general, _long) -> return true
                    TypeUtils.equalTypes(general, _byte) -> return true
                    TypeUtils.equalTypes(general, _short) -> return true
                }
            }
            TypeUtils.equalTypes(specific, _short) && TypeUtils.equalTypes(general, _byte) -> return true
        }

        return false
    }

}
