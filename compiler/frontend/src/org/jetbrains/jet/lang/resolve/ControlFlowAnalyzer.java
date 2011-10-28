package org.jetbrains.jet.lang.resolve;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.cfg.JetFlowInformationProvider;
import org.jetbrains.jet.lang.cfg.pseudocode.JetControlFlowDataTraceFactory;
import org.jetbrains.jet.lang.descriptors.ConstructorDescriptor;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptorImpl;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.JetStandardClasses;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.expressions.ExpressionTypingServices;
import org.jetbrains.jet.lexer.JetTokens;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.jetbrains.jet.lang.diagnostics.Errors.*;
import static org.jetbrains.jet.lang.types.TypeUtils.NO_EXPECTED_TYPE;

/**
 * @author svtk
 */
public class ControlFlowAnalyzer {
    private TopDownAnalysisContext context;
    private ExpressionTypingServices typeInferrerServices;
    private final JetControlFlowDataTraceFactory flowDataTraceFactory;
    private final boolean declaredLocally;

    public ControlFlowAnalyzer(TopDownAnalysisContext context, JetControlFlowDataTraceFactory flowDataTraceFactory, boolean declaredLocally) {
        this.context = context;
        this.flowDataTraceFactory = flowDataTraceFactory;
        this.typeInferrerServices = context.getSemanticServices().getTypeInferrerServices(context.getTrace());
        this.declaredLocally = declaredLocally;
    }

    public void process() {
        for (Map.Entry<JetNamedFunction, FunctionDescriptorImpl> entry : context.getFunctions().entrySet()) {
            JetNamedFunction function = entry.getKey();
            FunctionDescriptorImpl functionDescriptor = entry.getValue();

            final JetType expectedReturnType = !function.hasBlockBody() && !function.hasDeclaredReturnType()
                                               ? NO_EXPECTED_TYPE
                                               : functionDescriptor.getReturnType();
            checkFunction(function, functionDescriptor, expectedReturnType);
        }

        for (Map.Entry<JetDeclaration, ConstructorDescriptor> entry : this.context.getConstructors().entrySet()) {
            JetDeclaration declaration = entry.getKey();
            assert declaration instanceof JetConstructor;
            JetConstructor constructor = (JetConstructor) declaration;
            ConstructorDescriptor descriptor = entry.getValue();

            checkFunction(constructor, descriptor, JetStandardClasses.getUnitType());
        }

        for (JetProperty property : context.getProperties().keySet()) {
            checkProperty(property);
        }
    }

    private void checkFunction(JetDeclarationWithBody function, FunctionDescriptor functionDescriptor, final @NotNull JetType expectedReturnType) {
        assert function instanceof JetDeclaration;

        JetExpression bodyExpression = function.getBodyExpression();
        if (bodyExpression == null) return;
        JetFlowInformationProvider flowInformationProvider = new JetFlowInformationProvider((JetDeclaration) function, bodyExpression, flowDataTraceFactory, context.getTrace());

        final boolean blockBody = function.hasBlockBody();
        List<JetElement> unreachableElements = Lists.newArrayList();
        flowInformationProvider.collectUnreachableExpressions(function.asElement(), unreachableElements);

        // This is needed in order to highlight only '1 < 2' and not '1', '<' and '2' as well
        final Set<JetElement> rootUnreachableElements = JetPsiUtil.findRootExpressions(unreachableElements);

        for (JetElement element : rootUnreachableElements) {
            context.getTrace().report(UNREACHABLE_CODE.on(element));
        }

        List<JetExpression> returnedExpressions = Lists.newArrayList();
        flowInformationProvider.collectReturnExpressions(function.asElement(), returnedExpressions);

        boolean nothingReturned = returnedExpressions.isEmpty();

        returnedExpressions.remove(function); // This will be the only "expression" if the body is empty

        final JetScope scope = this.context.getDeclaringScopes().get(function);

        if (expectedReturnType != NO_EXPECTED_TYPE && !JetStandardClasses.isUnit(expectedReturnType) && returnedExpressions.isEmpty() && !nothingReturned) {
            context.getTrace().report(RETURN_TYPE_MISMATCH.on(bodyExpression, expectedReturnType));
        }

        for (JetExpression returnedExpression : returnedExpressions) {
            returnedExpression.accept(new JetVisitorVoid() {
                @Override
                public void visitReturnExpression(JetReturnExpression expression) {
                    if (!blockBody) {
                        context.getTrace().report(RETURN_IN_FUNCTION_WITH_EXPRESSION_BODY.on(expression));
                    }
                }

                @Override
                public void visitExpression(JetExpression expression) {
                    if (blockBody && expectedReturnType != NO_EXPECTED_TYPE && !JetStandardClasses.isUnit(expectedReturnType) && !rootUnreachableElements.contains(expression)) {
                        JetType type = typeInferrerServices.getType(scope, expression, expectedReturnType);
                        if (type == null || !JetStandardClasses.isNothing(type)) {
                            context.getTrace().report(NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY.on(expression));
                        }
                    }
                }
            });
        }
        if (!declaredLocally) {
            flowInformationProvider.markUninitializedVariables(function.asElement(), functionDescriptor.getValueParameters());

            if (((JetDeclaration) function).hasModifier(JetTokens.INLINE_KEYWORD)) {
                //inline functions after M1
//                flowInformationProvider.markNotOnlyInvokedFunctionVariables(function.asElement(), functionDescriptor.getValueParameters());
            }
        }
    }

    private void checkProperty(JetProperty property) {
        JetExpression initializer = property.getInitializer();
        if (initializer == null) return;
        new JetFlowInformationProvider(property, initializer, flowDataTraceFactory, context.getTrace());
    }
}
