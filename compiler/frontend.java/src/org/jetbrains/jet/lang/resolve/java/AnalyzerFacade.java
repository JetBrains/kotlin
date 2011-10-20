package org.jetbrains.jet.lang.resolve.java;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.cfg.pseudocode.JetControlFlowDataTraceFactory;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetNamespace;
import org.jetbrains.jet.lang.resolve.AnalyzingUtils;
import org.jetbrains.jet.lang.resolve.BindingContext;

/**
 * @author abreslav
 */
public class AnalyzerFacade {
    public static BindingContext analyzeNamespace(@NotNull JetNamespace namespace, @NotNull JetControlFlowDataTraceFactory flowDataTraceFactory) {
        return AnalyzingUtils.getInstance(JavaDefaultImports.JAVA_DEFAULT_IMPORTS).analyzeNamespace(namespace, flowDataTraceFactory);
    }

    public static BindingContext analyzeFileWithCache(@NotNull JetFile file) {
        return AnalyzingUtils.getInstance(JavaDefaultImports.JAVA_DEFAULT_IMPORTS).analyzeFileWithCache(file);
    }


}
