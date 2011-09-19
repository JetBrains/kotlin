package org.jetbrains.jet.lang.resolve.java;

import com.intellij.openapi.project.Project;
import org.jetbrains.jet.lang.JetSemanticServices;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.ImportingStrategy;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;

/**
 * @author abreslav
 */
public class JavaDefaultImports {
    public static final ImportingStrategy JAVA_DEFAULT_IMPORTS = new ImportingStrategy() {
        @Override
        public void addImports(Project project, JetSemanticServices semanticServices, BindingTrace trace, WritableScope rootScope) {
//            scope.importScope(javaSemanticServices.getDescriptorResolver().resolveNamespace("").getMemberScope());
//            scope.importScope(javaSemanticServices.getDescriptorResolver().resolveNamespace("java.lang").getMemberScope());
            JavaSemanticServices javaSemanticServices = new JavaSemanticServices(project, semanticServices, trace);
            rootScope.importScope(new JavaPackageScope("", null, javaSemanticServices));
            rootScope.importScope(new JavaPackageScope("java.lang", null, javaSemanticServices));
        }
    };
}
