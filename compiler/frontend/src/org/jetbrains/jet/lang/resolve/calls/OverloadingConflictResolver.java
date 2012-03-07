/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.resolve.calls;

import gnu.trove.THashSet;
import gnu.trove.TObjectHashingStrategy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.JetSemanticServices;
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.OverridingUtil;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.jetbrains.jet.lang.types.lang.JetStandardLibrary;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeUtils;

import java.util.List;
import java.util.Set;

/**
 * @author abreslav
 */
public class OverloadingConflictResolver {

    private final JetSemanticServices semanticServices;

    public OverloadingConflictResolver(@NotNull JetSemanticServices semanticServices) {
        this.semanticServices = semanticServices;
    }

    @Nullable
    public <D extends CallableDescriptor> ResolvedCallImpl<D> findMaximallySpecific(Set<ResolvedCallImpl<D>> candidates, boolean discriminateGenericDescriptors) {
        // Different autocasts may lead to the same candidate descriptor wrapped into different ResolvedCallImpl objects
        Set<ResolvedCallImpl<D>> maximallySpecific = new THashSet<ResolvedCallImpl<D>>(new TObjectHashingStrategy<ResolvedCallImpl<D>>() {
                    @Override
                    public boolean equals(ResolvedCallImpl<D> o1, ResolvedCallImpl<D> o2) {
                        return o1 == null ? o2 == null : o1.getResultingDescriptor().equals(o2.getResultingDescriptor());
                    }

                    @Override
                    public int computeHashCode(ResolvedCallImpl<D> object) {
                        return object == null ? 0 : object.getResultingDescriptor().hashCode();
                    }
                });
        meLoop:
        for (ResolvedCallImpl<D> candidateCall : candidates) {
            D me = candidateCall.getResultingDescriptor();
            for (ResolvedCallImpl<D> otherCall : candidates) {
                D other = otherCall.getResultingDescriptor();
                if (other == me) continue;
                if (!moreSpecific(me, other, discriminateGenericDescriptors) || moreSpecific(other, me, discriminateGenericDescriptors)) {
                    continue meLoop;
                }
            }
            maximallySpecific.add(candidateCall);
        }
        if (maximallySpecific.size() == 1) {
            ResolvedCallImpl<D> result = maximallySpecific.iterator().next();
            result.getTrace().commit();
            return result;
        }
        return null;
    }

    /**
     * Let < mean "more specific"
     * Subtype < supertype
     * Double < Float
     * Int < Long
     * Int < Short < Byte
     */
    private <Descriptor extends CallableDescriptor> boolean moreSpecific(Descriptor f, Descriptor g, boolean discriminateGenericDescriptors) {
        if (discriminateGenericDescriptors && !isGeneric(f) && isGeneric(g)) return true;
        if (OverridingUtil.overrides(f, g)) return true;
        if (OverridingUtil.overrides(g, f)) return false;

        ReceiverDescriptor receiverOfF = f.getReceiverParameter();
        ReceiverDescriptor receiverOfG = g.getReceiverParameter();
        if (f.getReceiverParameter().exists() && g.getReceiverParameter().exists()) {
            if (!typeMoreSpecific(receiverOfF.getType(), receiverOfG.getType())) return false;
        }

        List<ValueParameterDescriptor> fParams = f.getValueParameters();
        List<ValueParameterDescriptor> gParams = g.getValueParameters();

        int fSize = fParams.size();
        if (fSize != gParams.size()) return false;
        for (int i = 0; i < fSize; i++) {
            JetType fParamType = fParams.get(i).getType();
            JetType gParamType = gParams.get(i).getType();

            if (!typeMoreSpecific(fParamType, gParamType)) {
                return false;
            }
        }
        
        if (discriminateGenericDescriptors && isGeneric(f)) {
            if (!isGeneric(g)) {
                return false;
            }

            // g is generic, too

            return moreSpecific(DescriptorUtils.substituteBounds(f), DescriptorUtils.substituteBounds(g), false);
        }

        return true;
    }
    
    private boolean isGeneric(CallableDescriptor f) {
        return !f.getOriginal().getTypeParameters().isEmpty();
    }

    private boolean typeMoreSpecific(@NotNull JetType specific, @NotNull JetType general) {
        return semanticServices.getTypeChecker().isSubtypeOf(specific, general) ||
                            numericTypeMoreSpecific(specific, general);
    }

    private boolean numericTypeMoreSpecific(@NotNull JetType specific, @NotNull JetType general) {
        JetStandardLibrary standardLibrary = semanticServices.getStandardLibrary();
        JetType _double = standardLibrary.getDoubleType();
        JetType _float = standardLibrary.getFloatType();
        JetType _long = standardLibrary.getLongType();
        JetType _int = standardLibrary.getIntType();
        JetType _byte = standardLibrary.getByteType();
        JetType _short = standardLibrary.getShortType();

        if (TypeUtils.equalTypes(specific, _double) && TypeUtils.equalTypes(general, _float)) return true;
        if (TypeUtils.equalTypes(specific, _int)) {
            if (TypeUtils.equalTypes(general, _long)) return true;
            if (TypeUtils.equalTypes(general, _byte)) return true;
            if (TypeUtils.equalTypes(general, _short)) return true;
        }
        if (TypeUtils.equalTypes(specific, _short) && TypeUtils.equalTypes(general, _byte)) return true;
        return false;
    }

}
