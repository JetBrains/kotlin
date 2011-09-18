package org.jetbrains.jet.lang.resolve.calls;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor;
import org.jetbrains.jet.lang.psi.Call;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.JetType;

import java.util.Collection;
import java.util.List;

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

    public List<ResolutionTask<D>> computePrioritizedTasks(@NotNull JetScope scope, @Nullable JetType receiverType, @NotNull Call call, @NotNull String name) {
        List<ResolutionTask<D>> result = Lists.newArrayList();
        doComputeTasks(scope, receiverType, call, name, result);
        return result;
    }

    private void doComputeTasks(JetScope scope, JetType receiverType, Call call, String name, List<ResolutionTask<D>> result) {
        List<ReceiverDescriptor> receivers = Lists.newArrayList();
        scope.getImplicitReceiversHierarchy(receivers);
        if (receiverType != null) {
            Collection<D> extensionFunctions = getExtensionsByName(scope, name);
            List<D> nonlocals = Lists.newArrayList();
            List<D> locals = Lists.newArrayList();
            //noinspection unchecked,RedundantTypeArguments
            TaskPrioritizer.<D>splitLexicallyLocalDescriptors(extensionFunctions, scope.getContainingDeclaration(), locals, nonlocals);

            Collection<D> members = getMembersByName(receiverType, name);

            addTask(result, receiverType, call, locals);
            addTask(result, null, call, members);

            for (ReceiverDescriptor receiver : receivers) {
                Collection<D> memberExtensions = getExtensionsByName(receiver.getReceiverType().getMemberScope(), name);
                addTask(result, receiverType, call, memberExtensions);
            }

            addTask(result, receiverType, call, nonlocals);
        }
        else {
            Collection<D> functions = getNonExtensionsByName(scope, name);

            List<D> nonlocals = Lists.newArrayList();
            List<D> locals = Lists.newArrayList();
            //noinspection unchecked,RedundantTypeArguments
            TaskPrioritizer.<D>splitLexicallyLocalDescriptors(functions, scope.getContainingDeclaration(), locals, nonlocals);

            addTask(result, receiverType, call, locals);

            for (ReceiverDescriptor receiver : receivers) {
                doComputeTasks(scope, receiver.getReceiverType(), call, name, result);
            }

            addTask(result, receiverType, call, nonlocals);
        }
    }

    @NotNull
    protected abstract Collection<D> getNonExtensionsByName(JetScope scope, String name);

    @NotNull
    protected abstract Collection<D> getMembersByName(@NotNull JetType receiverType, String name);

    @NotNull
    protected abstract Collection<D> getExtensionsByName(JetScope scope, String name);

    private void addTask(@NotNull List<ResolutionTask<D>> result, @Nullable JetType receiverType, @NotNull Call call, @NotNull Collection<D> candidates) {
        if (candidates.isEmpty()) return;
        result.add(createTask(receiverType, call, candidates));
    }

    @NotNull
    protected abstract ResolutionTask<D> createTask(JetType receiverType, Call call, Collection<D> candidates);

}
