package org.jetbrains.jet.lang;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.psi.JetPsiFactory;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.Importer;
import org.jetbrains.jet.lang.resolve.ImportsResolver;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;

/**
 * @author svtk
 */
public class StandardConfiguration implements Configuration {
    private Project project;

    public static StandardConfiguration createStandardConfiguration(Project project) {
        return new StandardConfiguration(project);
    }

    private StandardConfiguration(Project project) {
        this.project = project;
    }

    @Override
    public void addDefaultImports(@NotNull BindingTrace trace, @NotNull WritableScope rootScope, @NotNull Importer importer) {
        ImportsResolver.ImportResolver importResolver = new ImportsResolver.ImportResolver(trace, true);
        importResolver.processImportReference(JetPsiFactory.createImportDirective(project, "std.*"), rootScope, importer);
    }

    @Override
    public void extendNamespaceScope(@NotNull BindingTrace trace, @NotNull NamespaceDescriptor namespaceDescriptor, @NotNull WritableScope namespaceMemberScope) {
    }

}
