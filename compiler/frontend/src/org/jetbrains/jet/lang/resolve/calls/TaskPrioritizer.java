package org.jetbrains.jet.lang.resolve.calls;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor;
import org.jetbrains.jet.lang.psi.Call;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.JetTypeChecker;
import org.jetbrains.jet.lang.types.NamespaceType;

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

    public List<ResolutionTask<D>> computePrioritizedTasks(@NotNull JetScope scope, @NotNull Call call, @NotNull String name, @NotNull BindingContext bindingContext, @NotNull DataFlowInfo dataFlowInfo) {
        List<ResolutionTask<D>> result = Lists.newArrayList();

        ReceiverDescriptor explicitReceiver = call.getExplicitReceiver();
        if (explicitReceiver.exists() && explicitReceiver.getType() instanceof NamespaceType) {
            scope = explicitReceiver.getType().getMemberScope();
            explicitReceiver = NO_RECEIVER;
        }
        doComputeTasks(scope, explicitReceiver, call, name, result, AutoCastService.NO_AUTO_CASTS);

        ReceiverDescriptor receiverToCast = explicitReceiver.exists() ? explicitReceiver : scope.getImplicitReceiver();
        if (receiverToCast.exists()) {
            doComputeTasks(scope, receiverToCast, call, name, result, new AutoCastServiceImpl(dataFlowInfo, bindingContext));
        }
        return result;
    }

    private void doComputeTasks(JetScope scope, ReceiverDescriptor receiver, Call call, String name, List<ResolutionTask<D>> result, @NotNull AutoCastService autoCastService) {
        DataFlowInfo dataFlowInfo = autoCastService.getDataFlowInfo();
        List<ReceiverDescriptor> implicitReceivers = Lists.newArrayList();
        scope.getImplicitReceiversHierarchy(implicitReceivers);
        if (receiver.exists()) {
            List<ReceiverDescriptor> variantsForExplicitReceiver = autoCastService.getVariantsForReceiver(receiver);

            Collection<ResolvedCall<D>> extensionFunctions = convertWithImpliedThis(scope, variantsForExplicitReceiver, getExtensionsByName(scope, name));
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
            Collection<ResolvedCall<D>> functions = convertWithImpliedThis(scope, Collections.singletonList(receiver), getNonExtensionsByName(scope, name));

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
                    resolvedCall.setReceiverArgument(receiverParameter);
                    result.add(resolvedCall);
                }
            }
        }
//        return result;
    }

    public static <D extends CallableDescriptor> Collection<ResolvedCall<D>> convertWithImpliedThis(JetScope scope, Iterable<ReceiverDescriptor> receiverParameters, Collection<D> descriptors) {
        Collection<ResolvedCall<D>> result = Lists.newArrayList();
        for (ReceiverDescriptor receiverParameter : receiverParameters) {
            for (D extension : descriptors) {
                ResolvedCall<D> resolvedCall = ResolvedCall.create(extension);
                resolvedCall.setReceiverArgument(receiverParameter);
                if (setImpliedThis(scope, resolvedCall)) {
                    result.add(resolvedCall);
                }
            }
        }
        return result;
    }

    private static <D extends CallableDescriptor> boolean setImpliedThis(@NotNull JetScope scope, ResolvedCall<D> resolvedCall) {
        ReceiverDescriptor expectedThisObject = resolvedCall.getCandidateDescriptor().getExpectedThisObject();
        if (!expectedThisObject.exists()) return true;
        List<ReceiverDescriptor> receivers = Lists.newArrayList();
        scope.getImplicitReceiversHierarchy(receivers);
        for (ReceiverDescriptor receiver : receivers) {
            if (JetTypeChecker.INSTANCE.isSubtypeOf(receiver.getType(), expectedThisObject.getType())) {
                // TODO : Autocasts & nullability
                resolvedCall.setThisObject(expectedThisObject);
                return true;
            }
        }
        return false;
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
