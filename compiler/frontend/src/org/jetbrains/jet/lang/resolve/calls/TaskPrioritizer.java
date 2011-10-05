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
import org.jetbrains.jet.lang.types.DataFlowInfo;

import java.util.Collection;
import java.util.List;

import static org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor.NO_RECEIVER;

/**
* @author abreslav
*/
/*package*/ abstract class TaskPrioritizer<D extends CallableDescriptor> {

    public static <T extends DeclarationDescriptor> void splitLexicallyLocalDescriptors(
            Collection<? extends T> allDescriptors, DeclarationDescriptor containerOfTheCurrentLocality, List<? super T> local, List<? super T> nonlocal) {

        for (T descriptor : allDescriptors) {
            if (isLocal(containerOfTheCurrentLocality, descriptor)) {
                local.add(descriptor);
            }
            else {
                nonlocal.add(descriptor);
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
    public static boolean isLocal(DeclarationDescriptor containerOfTheCurrentLocality, DeclarationDescriptor candidate) {
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

    public List<ResolutionTask<D>> computePrioritizedTasks(@NotNull JetScope scope, @NotNull ReceiverDescriptor receiver, @NotNull Call call, @NotNull String name, @NotNull BindingContext bindingContext, @NotNull DataFlowInfo dataFlowInfo) {
        List<ResolutionTask<D>> result = Lists.newArrayList();

        doComputeTasks(scope, receiver, call, name, result);

//        ReceiverDescriptor receiverToCast = receiver.exists() ? receiver : scope.getImplicitReceiver();
//        if (receiverToCast.exists()) {
//            for (ReceiverDescriptor castReceiver : AutoCastUtils.getAutocastVariants(bindingContext, dataFlowInfo, receiverToCast)) {
//                doComputeTasks(scope, castReceiver, call, name, result);
//            }
//        }
        return result;
    }

    private void doComputeTasks(JetScope scope, ReceiverDescriptor receiver, Call call, String name, List<ResolutionTask<D>> result) {
        List<ReceiverDescriptor> implicitReceivers = Lists.newArrayList();
        scope.getImplicitReceiversHierarchy(implicitReceivers);
        if (receiver.exists()) {
            Collection<D> extensionFunctions = getExtensionsByName(scope, name);
            List<D> nonlocals = Lists.newArrayList();
            List<D> locals = Lists.newArrayList();
            //noinspection unchecked,RedundantTypeArguments
            TaskPrioritizer.<D>splitLexicallyLocalDescriptors(extensionFunctions, scope.getContainingDeclaration(), locals, nonlocals);

            Collection<D> members = getMembersByName(receiver, name);

            addTask(result, receiver, call, locals);
            addTask(result, NO_RECEIVER, call, members);

            for (ReceiverDescriptor implicitReceiver : implicitReceivers) {
                Collection<D> memberExtensions = getExtensionsByName(implicitReceiver.getType().getMemberScope(), name);
                addTask(result, receiver, call, memberExtensions);
            }

            addTask(result, receiver, call, nonlocals);
        }
        else {
            Collection<D> functions = getNonExtensionsByName(scope, name);

            List<D> nonlocals = Lists.newArrayList();
            List<D> locals = Lists.newArrayList();
            //noinspection unchecked,RedundantTypeArguments
            TaskPrioritizer.<D>splitLexicallyLocalDescriptors(functions, scope.getContainingDeclaration(), locals, nonlocals);

            addTask(result, receiver, call, locals);

            for (ReceiverDescriptor implicitReceiver : implicitReceivers) {
                doComputeTasks(scope, implicitReceiver, call, name, result);
            }

            addTask(result, receiver, call, nonlocals);
        }
    }

    private void addTask(@NotNull List<ResolutionTask<D>> result, @NotNull ReceiverDescriptor receiver, @NotNull Call call, @NotNull Collection<D> candidateDescriptors) {
        if (candidateDescriptors.isEmpty()) return;
        result.add(createTask(receiver, call, ResolvedCall.convertCollection(candidateDescriptors)));
    }

    @NotNull
    protected abstract Collection<D> getNonExtensionsByName(JetScope scope, String name);

    @NotNull
    protected abstract Collection<D> getMembersByName(@NotNull ReceiverDescriptor receiver, String name);

    @NotNull
    protected abstract Collection<D> getExtensionsByName(JetScope scope, String name);

    @NotNull
    protected abstract ResolutionTask<D> createTask(ReceiverDescriptor receiver, Call call, Collection<ResolvedCall<D>> candidates);

}
