package org.jetbrains.jet.lang.resolve.calls;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor;
import org.jetbrains.jet.lang.psi.Call;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetSuperExpression;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.calls.autocasts.AutoCastService;
import org.jetbrains.jet.lang.resolve.calls.autocasts.AutoCastServiceImpl;
import org.jetbrains.jet.lang.resolve.calls.autocasts.DataFlowInfo;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ExpressionReceiver;
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
            Collection<ResolvedCallImpl<D>> allDescriptors, DeclarationDescriptor containerOfTheCurrentLocality, Collection<ResolvedCallImpl<D>> local, Collection<ResolvedCallImpl<D>> nonlocal) {

        for (ResolvedCallImpl<D> resolvedCall : allDescriptors) {
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

    @Nullable
    /*package*/ static JetSuperExpression getReceiverSuper(@NotNull ReceiverDescriptor receiver) {
        if (receiver instanceof ExpressionReceiver) {
            ExpressionReceiver expressionReceiver = (ExpressionReceiver) receiver;
            JetExpression expression = expressionReceiver.getExpression();
            if (expression instanceof JetSuperExpression) {
                return (JetSuperExpression) expression;
            }
        }
        return null;
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

            Collection<ResolvedCallImpl<D>> extensionFunctions = convertWithImpliedThis(scope, variantsForExplicitReceiver, getExtensionsByName(scope, name));
            List<ResolvedCallImpl<D>> nonlocals = Lists.newArrayList();
            List<ResolvedCallImpl<D>> locals = Lists.newArrayList();
            //noinspection unchecked,RedundantTypeArguments
            TaskPrioritizer.<D>splitLexicallyLocalDescriptors(extensionFunctions, scope.getContainingDeclaration(), locals, nonlocals);

            Collection<ResolvedCallImpl<D>> members = Lists.newArrayList();
            for (ReceiverDescriptor variant : variantsForExplicitReceiver) {
                Collection<D> membersForThisVariant = getMembersByName(variant.getType(), name);
                convertWithReceivers(membersForThisVariant, Collections.singletonList(variant), Collections.singletonList(NO_RECEIVER), members);
            }

            if (getReceiverSuper(receiver) != null) {
                // If the call is of the form super.foo(), it can actually be only a member
                // But  if there's no appropriate member, we would like to report that super cannot be a receiver for an extension
                // Thus, put members first
                addTask(result, call, members, dataFlowInfo);
                addTask(result, call, locals, dataFlowInfo);
            }
            else {
                addTask(result, call, locals, dataFlowInfo);
                addTask(result, call, members, dataFlowInfo);
            }

            for (ReceiverDescriptor implicitReceiver : implicitReceivers) {
                Collection<D> memberExtensions = getExtensionsByName(implicitReceiver.getType().getMemberScope(), name);
                List<ReceiverDescriptor> variantsForImplicitReceiver = autoCastService.getVariantsForReceiver(implicitReceiver);
                addTask(result, call, convertWithReceivers(memberExtensions, variantsForImplicitReceiver, variantsForExplicitReceiver), dataFlowInfo);
            }

            addTask(result, call, nonlocals, dataFlowInfo);
        }
        else {
            Collection<ResolvedCallImpl<D>> functions = convertWithImpliedThis(scope, Collections.singletonList(receiver), getNonExtensionsByName(scope, name));

            List<ResolvedCallImpl<D>> nonlocals = Lists.newArrayList();
            List<ResolvedCallImpl<D>> locals = Lists.newArrayList();
            //noinspection unchecked,RedundantTypeArguments
            TaskPrioritizer.<D>splitLexicallyLocalDescriptors(functions, scope.getContainingDeclaration(), locals, nonlocals);

            addTask(result, call, locals, dataFlowInfo);

            for (ReceiverDescriptor implicitReceiver : implicitReceivers) {
                doComputeTasks(scope, implicitReceiver, call, name, result, autoCastService);
            }

            addTask(result, call, nonlocals, dataFlowInfo);
        }
    }

    private Collection<ResolvedCallImpl<D>> convertWithReceivers(Collection<D> descriptors, Iterable<ReceiverDescriptor> thisObjects, Iterable<ReceiverDescriptor> receiverParameters) {
        Collection<ResolvedCallImpl<D>> result = Lists.newArrayList();
        convertWithReceivers(descriptors, thisObjects, receiverParameters, result);
        return result;
    }

    private void convertWithReceivers(Collection<D> descriptors, Iterable<ReceiverDescriptor> thisObjects, Iterable<ReceiverDescriptor> receiverParameters, Collection<ResolvedCallImpl<D>> result) {
//        Collection<ResolvedCallImpl<D>> result = Lists.newArrayList();
        for (ReceiverDescriptor thisObject : thisObjects) {
            for (ReceiverDescriptor receiverParameter : receiverParameters) {
                for (D extension : descriptors) {
                    ResolvedCallImpl<D> resolvedCall = ResolvedCallImpl.create(extension);
                    resolvedCall.setThisObject(thisObject);
                    resolvedCall.setReceiverArgument(receiverParameter);
                    result.add(resolvedCall);
                }
            }
        }
//        return result;
    }

    public static <D extends CallableDescriptor> Collection<ResolvedCallImpl<D>> convertWithImpliedThis(JetScope scope, Iterable<ReceiverDescriptor> receiverParameters, Collection<D> descriptors) {
        Collection<ResolvedCallImpl<D>> result = Lists.newArrayList();
        for (ReceiverDescriptor receiverParameter : receiverParameters) {
            for (D extension : descriptors) {
                ResolvedCallImpl<D> resolvedCall = ResolvedCallImpl.create(extension);
                resolvedCall.setReceiverArgument(receiverParameter);
                if (setImpliedThis(scope, resolvedCall)) {
                    result.add(resolvedCall);
                }
            }
        }
        return result;
    }

    private static <D extends CallableDescriptor> boolean setImpliedThis(@NotNull JetScope scope, ResolvedCallImpl<D> resolvedCall) {
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

   private void addTask(@NotNull List<ResolutionTask<D>> result, @NotNull Call call, @NotNull Collection<ResolvedCallImpl<D>> candidates, @NotNull DataFlowInfo dataFlowInfo) {
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
