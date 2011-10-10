package org.jetbrains.jet.lang.resolve.calls;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.Call;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.jetbrains.jet.lang.types.DataFlowInfo;
import org.jetbrains.jet.lang.types.JetType;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor.NO_RECEIVER;

/**
* @author abreslav
*/
/*package*/ abstract class TaskPrioritizer<D extends CallableDescriptor> {

    public static <D extends CallableDescriptor> void splitLexicallyLocalDescriptors(
            Collection<ResolvedCall<D>> allDescriptors, DeclarationDescriptor containerOfTheCurrentLocality, Collection<ResolvedCall<D>> local, Collection<ResolvedCall<D>> nonlocal) {

        for (ResolvedCall<D> resolvedCall : allDescriptors) {
            if (isLocal(containerOfTheCurrentLocality, resolvedCall.getCandidateDescriptor())) {
                local.add(resolvedCall);
            }
            else {
                nonlocal.add(resolvedCall);
            }
        }
    }

    /**
     * The primary case for local extensions is the following:
     *
     * I had a locally declared extension function or a local variable of function type called foo
     * And I called it on my x
     * Now, someone added function foo() to the class of x
     * My code should not change
     *
     * thus
     *
     * local extension prevail over members (and members prevail over all non-local extensions)
     */
    private static boolean isLocal(DeclarationDescriptor containerOfTheCurrentLocality, DeclarationDescriptor candidate) {
        if (candidate instanceof ValueParameterDescriptor) {
            return true;
        }
        DeclarationDescriptor parent = candidate.getContainingDeclaration();
        if (!(parent instanceof FunctionDescriptor)) {
            return false;
        }
        FunctionDescriptor functionDescriptor = (FunctionDescriptor) parent;
        DeclarationDescriptor current = containerOfTheCurrentLocality;
        while (current != null) {
            if (current == functionDescriptor) {
                return true;
            }
            current = current.getContainingDeclaration();
        }
        return false;
    }

//    public List<ResolutionTask<D>> computePrioritizedTasks(@NotNull JetScope scope, @NotNull ReceiverDescriptor receiver, @NotNull Call call, @NotNull String name) {
//        List<ResolutionTask<D>> result = Lists.newArrayList();
//        doComputeTasks(scope, receiver, call, name, result);
//        return result;
//    }

    public List<ResolutionTask<D>> computePrioritizedTasks(@NotNull JetScope scope, @NotNull Call call, @NotNull String name, @NotNull BindingContext bindingContext, @NotNull DataFlowInfo dataFlowInfo) {
        List<ResolutionTask<D>> result = Lists.newArrayList();

        ReceiverDescriptor explicitReceiver = call.getExplicitReceiver();
        doComputeTasks(scope, explicitReceiver, call, name, result, NO_AUTO_CASTS);

        ReceiverDescriptor receiverToCast = explicitReceiver.exists() ? explicitReceiver : scope.getImplicitReceiver();
        if (receiverToCast.exists()) {
            doComputeTasks(scope, receiverToCast, call, name, result, new AutoCastServiceImpl(dataFlowInfo, bindingContext));
        }
        return result;
    }

    private interface AutoCastService {
        List<ReceiverDescriptor> getVariantsForReceiver(ReceiverDescriptor receiverDescriptor);
        DataFlowInfo getDataFlowInfo();
    }

    private static class AutoCastServiceImpl implements AutoCastService {
        private final DataFlowInfo dataFlowInfo;
        private final BindingContext bindingContext;

        private AutoCastServiceImpl(DataFlowInfo dataFlowInfo, BindingContext bindingContext) {
            this.dataFlowInfo = dataFlowInfo;
            this.bindingContext = bindingContext;
        }

        @Override
        public List<ReceiverDescriptor> getVariantsForReceiver(ReceiverDescriptor receiverDescriptor) {
            return AutoCastUtils.getAutoCastVariants(bindingContext, dataFlowInfo, receiverDescriptor);
        }

        @Override
        public DataFlowInfo getDataFlowInfo() {
            return dataFlowInfo;
        }
    }

    private static AutoCastService NO_AUTO_CASTS = new AutoCastService() {
        @Override
        public DataFlowInfo getDataFlowInfo() {
            return DataFlowInfo.getEmpty();
        }

        @Override
        public List<ReceiverDescriptor> getVariantsForReceiver(ReceiverDescriptor receiverDescriptor) {
            return Collections.singletonList(receiverDescriptor);
        }
    };
    
    private void doComputeTasks(JetScope scope, ReceiverDescriptor explicitReceiver, Call call, String name, List<ResolutionTask<D>> result, @NotNull AutoCastService autoCastService) {
        DataFlowInfo dataFlowInfo = autoCastService.getDataFlowInfo();
        List<ReceiverDescriptor> implicitReceivers = Lists.newArrayList();
        scope.getImplicitReceiversHierarchy(implicitReceivers);
        // AutoCastUtils.getAutoCastVariants(bindingContext, dataFlowInfo, receiverToCast)
        if (explicitReceiver.exists()) {
            List<ReceiverDescriptor> variantsForExplicitReceiver = autoCastService.getVariantsForReceiver(explicitReceiver);

            Collection<ResolvedCall<D>> extensionFunctions = convertWithImpliedThis(variantsForExplicitReceiver, getExtensionsByName(scope, name));
            List<ResolvedCall<D>> nonlocals = Lists.newArrayList();
            List<ResolvedCall<D>> locals = Lists.newArrayList();
            //noinspection unchecked,RedundantTypeArguments
            TaskPrioritizer.<D>splitLexicallyLocalDescriptors(extensionFunctions, scope.getContainingDeclaration(), locals, nonlocals);

            Collection<ResolvedCall<D>> members = Lists.newArrayList();
            for (ReceiverDescriptor variant : variantsForExplicitReceiver) {
                Collection<D> membersForThisVariant = getMembersByName(variant.getType(), name);
                convertWithReceivers(membersForThisVariant, Collections.singletonList(variant), Collections.singletonList(NO_RECEIVER), members);
            }

            addTask(result, call, locals, dataFlowInfo);
            addTask(result, call, members, dataFlowInfo);

            for (ReceiverDescriptor implicitReceiver : implicitReceivers) {
                Collection<D> memberExtensions = getExtensionsByName(implicitReceiver.getType().getMemberScope(), name);
                List<ReceiverDescriptor> variantsForImplicitReceiver = autoCastService.getVariantsForReceiver(implicitReceiver);
                addTask(result, call, convertWithReceivers(memberExtensions, variantsForImplicitReceiver, variantsForExplicitReceiver), dataFlowInfo);
            }

            addTask(result, call, nonlocals, dataFlowInfo);
        }
        else {
            Collection<ResolvedCall<D>> functions = convertWithImpliedThis(Collections.singletonList(explicitReceiver), getNonExtensionsByName(scope, name));

            List<ResolvedCall<D>> nonlocals = Lists.newArrayList();
            List<ResolvedCall<D>> locals = Lists.newArrayList();
            //noinspection unchecked,RedundantTypeArguments
            TaskPrioritizer.<D>splitLexicallyLocalDescriptors(functions, scope.getContainingDeclaration(), locals, nonlocals);

            addTask(result, call, locals, dataFlowInfo);

            for (ReceiverDescriptor implicitReceiver : implicitReceivers) {
                doComputeTasks(scope, implicitReceiver, call, name, result, autoCastService);
            }

            addTask(result, call, nonlocals, dataFlowInfo);
        }
    }

    private Collection<ResolvedCall<D>> convertWithReceivers(Collection<D> descriptors, Iterable<ReceiverDescriptor> thisObjects, Iterable<ReceiverDescriptor> receiverParameters) {
        Collection<ResolvedCall<D>> result = Lists.newArrayList();
        convertWithReceivers(descriptors, thisObjects, receiverParameters, result);
        return result;
    }

    private void convertWithReceivers(Collection<D> descriptors, Iterable<ReceiverDescriptor> thisObjects, Iterable<ReceiverDescriptor> receiverParameters, Collection<ResolvedCall<D>> result) {
//        Collection<ResolvedCall<D>> result = Lists.newArrayList();
        for (ReceiverDescriptor thisObject : thisObjects) {
            for (ReceiverDescriptor receiverParameter : receiverParameters) {
                for (D extension : descriptors) {
                    ResolvedCall<D> resolvedCall = ResolvedCall.create(extension);
                    resolvedCall.setThisObject(thisObject);
                    resolvedCall.setReceiverParameter(receiverParameter);
                    result.add(resolvedCall);
                }
            }
        }
//        return result;
    }

    private Collection<ResolvedCall<D>> convertWithImpliedThis(Iterable<ReceiverDescriptor> receiverParameters, Collection<D> descriptors) {
        Collection<ResolvedCall<D>> result = Lists.newArrayList();
        for (ReceiverDescriptor receiverParameter : receiverParameters) {
            for (D extension : descriptors) {
                ResolvedCall<D> resolvedCall = ResolvedCall.create(extension);
                resolvedCall.setReceiverParameter(receiverParameter);
                setImpliedThis(resolvedCall);
                result.add(resolvedCall);
            }
        }
        return result;
    }

    private void setImpliedThis(ResolvedCall<D> resolvedCall) {
        ReceiverDescriptor thisObject;
        DeclarationDescriptor containingDeclaration = resolvedCall.getCandidateDescriptor().getContainingDeclaration();
        if (containingDeclaration instanceof ClassDescriptor) {
            ClassDescriptor classDescriptor = (ClassDescriptor) containingDeclaration;
            thisObject = classDescriptor.getImplicitReceiver();
        }
        else {
            thisObject = NO_RECEIVER;
        }
        resolvedCall.setThisObject(thisObject);
    }

    private void addTask(@NotNull List<ResolutionTask<D>> result, @NotNull Call call, @NotNull Collection<ResolvedCall<D>> candidates, @NotNull DataFlowInfo dataFlowInfo) {
        if (candidates.isEmpty()) return;
        result.add(new ResolutionTask<D>(candidates, call, dataFlowInfo));
    }

    @NotNull
    protected abstract Collection<D> getNonExtensionsByName(JetScope scope, String name);

    @NotNull
    protected abstract Collection<D> getMembersByName(@NotNull JetType receiver, String name);

    @NotNull
    protected abstract Collection<D> getExtensionsByName(JetScope scope, String name);
}
