package org.jetbrains.jet.lang.resolve.java;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.Configuration;
import org.jetbrains.jet.lang.JetSemanticServices;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.Importer;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;

/**
 * @author abreslav
 */
public class JavaBridgeConfiguration implements Configuration {

    public static Configuration createJavaBridgeConfiguration(@NotNull Project project, @NotNull BindingTrace trace, Configuration delegateConfiguration) {
        return new JavaBridgeConfiguration(project, trace, delegateConfiguration);
    }

    private final JavaSemanticServices javaSemanticServices;
    private final Configuration delegateConfiguration;

    private JavaBridgeConfiguration(Project project, BindingTrace trace, Configuration delegateConfiguration) {
        this.javaSemanticServices = new JavaSemanticServices(project, JetSemanticServices.createSemanticServices(project), trace);
        this.delegateConfiguration = delegateConfiguration;
    }

    @Override
    public void addDefaultImports(@NotNull BindingTrace trace, @NotNull WritableScope rootScope, @NotNull Importer importer) {
        rootScope.importScope(new JavaPackageScope("", null, javaSemanticServices));
        importer.addScopeImport(new JavaPackageScope("java.lang", null, javaSemanticServices));
        delegateConfiguration.addDefaultImports(trace, rootScope, importer);
    }

    @Override
    public void extendNamespaceScope(@NotNull BindingTrace trace, @NotNull NamespaceDescriptor namespaceDescriptor, @NotNull WritableScope namespaceMemberScope) {
        namespaceMemberScope.importScope(new JavaPackageScope(DescriptorUtils.getFQName(namespaceDescriptor), namespaceDescriptor, javaSemanticServices));
        delegateConfiguration.extendNamespaceScope(trace, namespaceDescriptor, namespaceMemberScope);
    }
}
