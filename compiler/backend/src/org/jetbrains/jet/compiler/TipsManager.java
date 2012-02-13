package org.jetbrains.jet.compiler;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetImportDirective;
import org.jetbrains.jet.lang.psi.JetNamespaceHeader;
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.calls.inference.ConstraintResolutionListener;
import org.jetbrains.jet.lang.resolve.calls.inference.ConstraintSystem;
import org.jetbrains.jet.lang.resolve.calls.inference.ConstraintSystemImpl;
import org.jetbrains.jet.lang.resolve.calls.inference.ConstraintSystemSolution;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.JetScopeUtils;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ExpressionReceiver;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.NamespaceType;
import org.jetbrains.jet.lang.types.Variance;

import java.util.*;

import static org.jetbrains.jet.lang.resolve.calls.inference.ConstraintType.RECEIVER;

/**
 * @author Nikolay Krasko, Alefas
 */
public final class TipsManager {

    private TipsManager() {
    }

    @NotNull
    public static Collection<DeclarationDescriptor> getReferenceVariants(JetSimpleNameExpression expression, BindingContext context) {
        JetExpression receiverExpression = expression.getReceiverExpression();
        if (receiverExpression != null) {
            // Process as call expression
            final JetScope resolutionScope = context.get(BindingContext.RESOLUTION_SCOPE, receiverExpression);
            final JetType expressionType = context.get(BindingContext.EXPRESSION_TYPE, receiverExpression);

            if (expressionType != null && resolutionScope != null) {
                return includeExternalCallableExtensions(
                        excludePrivateDescriptors(expressionType.getMemberScope().getAllDescriptors()),
                        resolutionScope, new ExpressionReceiver(receiverExpression, expressionType));
            }
        } else {
            JetScope resolutionScope = context.get(BindingContext.RESOLUTION_SCOPE, expression);
            if (resolutionScope != null) {
                if (expression.getParent() instanceof JetImportDirective || expression.getParent() instanceof JetNamespaceHeader) {
                    return excludeNonPackageDescriptors(resolutionScope.getAllDescriptors());
                } else {
                    HashSet<DeclarationDescriptor> descriptorsSet = Sets.newHashSet();

                    ArrayList<ReceiverDescriptor> result = new ArrayList<ReceiverDescriptor>();
                    resolutionScope.getImplicitReceiversHierarchy(result);

                    for (ReceiverDescriptor receiverDescriptor : result) {
                        JetType receiverType = receiverDescriptor.getType();
                        descriptorsSet.addAll(receiverType.getMemberScope().getAllDescriptors());
                    }

                    descriptorsSet.addAll(resolutionScope.getAllDescriptors());
                    return excludeNotCallableExtensions(excludePrivateDescriptors(descriptorsSet), resolutionScope);
                }
            }
        }

        return Collections.emptyList();
    }



    public static Collection<DeclarationDescriptor> excludePrivateDescriptors(
            @NotNull Collection<DeclarationDescriptor> descriptors) {

        return Collections2.filter(descriptors, new Predicate<DeclarationDescriptor>() {
            @Override
            public boolean apply(@Nullable DeclarationDescriptor descriptor) {
                if (descriptor == null) {
                    return false;
                }

                if (descriptor instanceof NamespaceDescriptor) {
                    NamespaceDescriptor namespaceDescriptor = (NamespaceDescriptor) descriptor;
                    if (namespaceDescriptor.getName().isEmpty()) {
                        return false;
                    }
                }

                return true;
            }
        });
    }

    public static Collection<DeclarationDescriptor> excludeNotCallableExtensions(
            @NotNull Collection<? extends DeclarationDescriptor> descriptors, @NotNull final JetScope scope
    ) {
        final Set<DeclarationDescriptor> descriptorsSet = Sets.newHashSet(descriptors);

        final ArrayList<ReceiverDescriptor> result = new ArrayList<ReceiverDescriptor>();
        scope.getImplicitReceiversHierarchy(result);

        descriptorsSet.removeAll(
                Collections2.filter(JetScopeUtils.getAllExtensions(scope), new Predicate<CallableDescriptor>() {
                    @Override
                    public boolean apply(CallableDescriptor callableDescriptor) {
                        if (!callableDescriptor.getReceiverParameter().exists()) {
                            return false;
                        }
                        for (ReceiverDescriptor receiverDescriptor : result) {
                            if (checkReceiverResolution(receiverDescriptor, callableDescriptor)) return false;
                        }
                        return true;
                    }
                }));

        return Lists.newArrayList(descriptorsSet);
    }
    
    private static Collection<DeclarationDescriptor> excludeNonPackageDescriptors(
            @NotNull Collection<DeclarationDescriptor> descriptors) {
        return Collections2.filter(descriptors, new Predicate<DeclarationDescriptor>() {
            @Override
            public boolean apply(DeclarationDescriptor declarationDescriptor) {
                return declarationDescriptor instanceof NamespaceDescriptor;
            }
        });
    }

    private static Collection<DeclarationDescriptor> includeExternalCallableExtensions(
            @NotNull Collection<DeclarationDescriptor> descriptors,
            @NotNull final JetScope externalScope,
            @NotNull final ReceiverDescriptor receiverDescriptor
    ) {
        // It's impossible to add extension function for namespace
        if (receiverDescriptor.getType() instanceof NamespaceType) {
            return descriptors;
        }

        Set<DeclarationDescriptor> descriptorsSet = Sets.newHashSet(descriptors);

        descriptorsSet.addAll(
                Collections2.filter(JetScopeUtils.getAllExtensions(externalScope),
                                                  new Predicate<CallableDescriptor>() {
                                                      @Override
                                                      public boolean apply(CallableDescriptor callableDescriptor) {
                                                          return checkReceiverResolution(receiverDescriptor, callableDescriptor);
                                                      }
                                                  }));

        return descriptorsSet;
    }

    /*
     * Checks if receiver declaration could be resolved to call expected receiver.
     */
    private static boolean checkReceiverResolution (
            @NotNull ReceiverDescriptor expectedReceiver,
            @NotNull CallableDescriptor receiverArgument
    ) {
        ConstraintSystem constraintSystem = new ConstraintSystemImpl(ConstraintResolutionListener.DO_NOTHING);
        for (TypeParameterDescriptor typeParameterDescriptor : receiverArgument.getTypeParameters()) {
            constraintSystem.registerTypeVariable(typeParameterDescriptor, Variance.INVARIANT);
        }

        ReceiverDescriptor receiverParameter = receiverArgument.getReceiverParameter();
        if (expectedReceiver.exists() && receiverParameter.exists()) {
            constraintSystem.addSubtypingConstraint(
                    RECEIVER.assertSubtyping(expectedReceiver.getType(), receiverParameter.getType()));
        }
        else if (expectedReceiver.exists() || receiverParameter.exists()) {
            // Only one of receivers exist
            return false;
        }

        ConstraintSystemSolution solution = constraintSystem.solve();
        return solution.getStatus().isSuccessful();
    }

}
