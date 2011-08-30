package org.jetbrains.jet.lang.resolve.calls;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.psi.Call;
import org.jetbrains.jet.lang.resolve.JetScope;
import org.jetbrains.jet.lang.types.JetType;

import java.util.Collection;
import java.util.List;

/**
* @author abreslav
*/
/*package*/ abstract class TaskPrioritizer<Descriptor extends CallableDescriptor> {

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

    public List<ResolutionTask<Descriptor>> computePrioritizedTasks(JetScope scope, JetType receiverType, Call call, String name) {
        List<ResolutionTask<Descriptor>> result = Lists.newArrayList();
        if (receiverType != null) {
            Collection<Descriptor> extensionFunctions = getExtensionsByName(scope, name);

            List<Descriptor> nonlocals = Lists.newArrayList();
            List<Descriptor> locals = Lists.newArrayList();
            splitLexicallyLocalDescriptors(extensionFunctions, scope.getContainingDeclaration(), locals, nonlocals);

            Collection<Descriptor> members = getMembersByName(receiverType, name);

            addTask(result, receiverType, call, locals);
            addTask(result, null, call, members);
            addTask(result, receiverType, call, nonlocals);
        }
        else {
            Collection<Descriptor> functions = getNonExtensionsByName(scope, name);

            List<Descriptor> nonlocals = Lists.newArrayList();
            List<Descriptor> locals = Lists.newArrayList();
            splitLexicallyLocalDescriptors(functions, scope.getContainingDeclaration(), locals, nonlocals);

            addTask(result, receiverType, call, locals);

            addTask(result, receiverType, call, nonlocals);
        }
        return result;
    }

    @NotNull
    protected abstract Collection<Descriptor> getNonExtensionsByName(JetScope scope, String name);

    @NotNull
    protected abstract Collection<Descriptor> getMembersByName(@NotNull JetType receiverType, String name);

    @NotNull
    protected abstract Collection<Descriptor> getExtensionsByName(JetScope scope, String name);

    private void addTask(@NotNull List<ResolutionTask<Descriptor>> result, @Nullable JetType receiverType, @NotNull Call call, @NotNull Collection<Descriptor> candidates) {
        if (candidates.isEmpty()) return;
        result.add(createTask(receiverType, call, candidates));
    }

    @NotNull
    protected abstract ResolutionTask<Descriptor> createTask(JetType receiverType, Call call, Collection<Descriptor> candidates);

}
