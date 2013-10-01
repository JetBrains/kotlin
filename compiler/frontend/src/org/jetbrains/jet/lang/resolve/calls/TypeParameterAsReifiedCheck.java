package org.jetbrains.jet.lang.resolve.calls;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;
import org.jetbrains.jet.lang.diagnostics.Errors;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.resolve.calls.context.BasicCallResolutionContext;
import org.jetbrains.jet.lang.resolve.calls.results.OverloadResolutionResultsImpl;
import org.jetbrains.jet.lang.types.JetType;

import java.util.Map;

public class TypeParameterAsReifiedCheck implements CallResolverExtension {
    @Override
    public <F extends CallableDescriptor> void run(
            @NotNull OverloadResolutionResultsImpl<F> results, @NotNull BasicCallResolutionContext context
    ) {
        if (results.isSuccess()) {
            Map<TypeParameterDescriptor, JetType> typeArguments = results.getResultingCall().getTypeArguments();
            for (Map.Entry<TypeParameterDescriptor, JetType> entry : typeArguments.entrySet()) {
                TypeParameterDescriptor parameter = entry.getKey();
                JetType argument = entry.getValue();

                if (parameter.isReified() && argument.getConstructor().getDeclarationDescriptor() instanceof TypeParameterDescriptor) {
                    JetExpression callee = context.call.getCalleeExpression();
                    PsiElement element = callee != null ? callee : context.call.getCallElement();
                    context.trace.report(Errors.TYPE_PARAMETER_AS_REIFIED.on(element, typeArguments.keySet().iterator().next()));
                }
            }
        }
    }
}
