package org.jetbrains.jet.lang.resolve;

import com.intellij.openapi.project.Project;
import org.jetbrains.jet.lang.JetSemanticServices;

/**
* @author abreslav
*/
public interface ImportingStrategy {
    ImportingStrategy NONE = new ImportingStrategy() {
        @Override
        public void addImports(Project project, JetSemanticServices semanticServices, BindingTrace trace, WritableScope rootScope) {

        }
    };

    void addImports(Project project, JetSemanticServices semanticServices, BindingTrace trace, WritableScope rootScope);
}
