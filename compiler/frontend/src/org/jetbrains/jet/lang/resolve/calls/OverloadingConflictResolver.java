package org.jetbrains.jet.lang.resolve.calls;

import com.google.common.collect.Maps;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.JetSemanticServices;
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.OverridingUtil;
import org.jetbrains.jet.lang.resolve.TemporaryBindingTrace;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.jetbrains.jet.lang.types.JetStandardLibrary;
import org.jetbrains.jet.lang.types.JetType;

import java.util.List;
import java.util.Map;

/**
 * @author abreslav
 */
public class OverloadingConflictResolver {

    private final JetSemanticServices semanticServices;

    public OverloadingConflictResolver(@NotNull JetSemanticServices semanticServices) {
        this.semanticServices = semanticServices;
    }

    @Nullable
    public <Descriptor extends CallableDescriptor> Descriptor findMaximallySpecific(Map<Descriptor, Descriptor> candidates, Map<Descriptor, TemporaryBindingTrace> traces, boolean discriminateGenericDescriptors) {
        Map<Descriptor, TemporaryBindingTrace> maximallySpecific = Maps.newHashMap();
        meLoop:
        for (Map.Entry<Descriptor, Descriptor> myEntry : candidates.entrySet()) {
            Descriptor me = myEntry.getValue();
            TemporaryBindingTrace myTrace = traces.get(myEntry.getKey());
            for (Descriptor other : candidates.values()) {
                if (other == me) continue;
                if (!moreSpecific(me, other, discriminateGenericDescriptors) || moreSpecific(other, me, discriminateGenericDescriptors)) continue meLoop;
            }
            maximallySpecific.put(me, myTrace);
        }
        if (maximallySpecific.size() == 1) {
            Map.Entry<Descriptor, TemporaryBindingTrace> result = maximallySpecific.entrySet().iterator().next();
            result.getValue().commit();
            return result.getKey();
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
        if (OverridingUtil.overrides(f, g)) return true;
        if (OverridingUtil.overrides(g, f)) return false;

        ReceiverDescriptor receiverOfF = f.getReceiver();
        ReceiverDescriptor receiverOfG = g.getReceiver();
        if (f.getReceiver().exists() && g.getReceiver().exists()) {
            if (!typeMoreSpecific(receiverOfF.getType(), receiverOfG.getType())) return false;
        }

        List<ValueParameterDescriptor> fParams = f.getValueParameters();
        List<ValueParameterDescriptor> gParams = g.getValueParameters();

        int fSize = fParams.size();
        if (fSize != gParams.size()) return false;
        for (int i = 0; i < fSize; i++) {
            JetType fParamType = fParams.get(i).getOutType();
            JetType gParamType = gParams.get(i).getOutType();

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

        if (semanticServices.getTypeChecker().equalTypes(specific, _double) && semanticServices.getTypeChecker().equalTypes(general, _float)) return true;
        if (semanticServices.getTypeChecker().equalTypes(specific, _int)) {
            if (semanticServices.getTypeChecker().equalTypes(general, _long)) return true;
            if (semanticServices.getTypeChecker().equalTypes(general, _byte)) return true;
            if (semanticServices.getTypeChecker().equalTypes(general, _short)) return true;
        }
        if (semanticServices.getTypeChecker().equalTypes(specific, _short) && semanticServices.getTypeChecker().equalTypes(general, _byte)) return true;
        return false;
    }

}
