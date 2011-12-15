package org.jetbrains.jet.lang;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.psi.JetImportDirective;
import org.jetbrains.jet.lang.psi.JetPsiFactory;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.ImportsResolver;
import org.jetbrains.jet.lang.resolve.TemporaryBindingTrace;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;

/**
 * @author svtk
 */
public class StandardConfiguration implements Configuration {
    private Project project;
    private JetSemanticServices services;

    public static StandardConfiguration createStandardConfiguration(Project project, JetSemanticServices services) {
        return new StandardConfiguration(services, project);
    }

    private StandardConfiguration(JetSemanticServices services, Project project) {
        this.services = services;
        this.project = project;
    }

    @Override
    public void addDefaultImports(@NotNull BindingTrace trace, @NotNull WritableScope rootScope) {
        TemporaryBindingTrace temporaryTrace = TemporaryBindingTrace.create(trace);
        JetImportDirective importDirective = JetPsiFactory.createImportDirective(project, "std.*");
        if (ImportsResolver.importNamespace(importDirective, rootScope, temporaryTrace, services)) {
            temporaryTrace.commit();
        }
    }

    @Override
    public void extendNamespaceScope(@NotNull BindingTrace trace, @NotNull NamespaceDescriptor namespaceDescriptor, @NotNull WritableScope namespaceMemberScope) {
    }

}
