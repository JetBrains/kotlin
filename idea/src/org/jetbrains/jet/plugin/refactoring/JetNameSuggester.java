package org.jetbrains.jet.plugin.refactoring;

import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.java.AnalyzerFacade;
import org.jetbrains.jet.lang.types.JetStandardLibrary;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.checker.JetTypeChecker;

/**
 * User: Alefas
 * Date: 31.01.12
 */
public class JetNameSuggester {
    public static String[] suggestNames(JetExpression expression) {
        String[] def = {"i"};
        BindingContext bindingContext = AnalyzerFacade.analyzeFileWithCache((JetFile) expression.getContainingFile(),
                                                                            AnalyzerFacade.SINGLE_DECLARATION_PROVIDER);
        JetType jetType = bindingContext.get(BindingContext.EXPRESSION_TYPE, expression);
        if (jetType == null) return def;
        
        JetType booleanType = JetStandardLibrary.getJetStandardLibrary(expression.getProject()).getBooleanType();
        if (JetTypeChecker.INSTANCE.equalTypes(jetType, booleanType)) {
            return new String[] {"b"};
        }
        return new String[] {"i"}; //todo:
    }
}
