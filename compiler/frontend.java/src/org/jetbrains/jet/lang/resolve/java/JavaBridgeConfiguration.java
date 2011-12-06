package org.jetbrains.jet.lang.resolve.java;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.Configuration;
import org.jetbrains.jet.lang.JetSemanticServices;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;

/**
 * @author abreslav
 */
public class JavaBridgeConfiguration implements Configuration {

    public static Configuration createJavaBridgeConfiguration(@NotNull Project project, @NotNull BindingTrace trace) {
        return new JavaBridgeConfiguration(project, trace);
    }

    private final JavaSemanticServices javaSemanticServices;

    private JavaBridgeConfiguration(Project project, BindingTrace trace) {
        this.javaSemanticServices = new JavaSemanticServices(project, JetSemanticServices.createSemanticServices(project), trace);
    }

    @Override
    public void addDefaultImports(BindingTrace trace, WritableScope rootScope) {
        rootScope.importScope(new JavaPackageScope("", null, javaSemanticServices));
        rootScope.importScope(new JavaPackageScope("java.lang", null, javaSemanticServices));
    }

    @Override
    public void extendNamespaceScope(BindingTrace trace, @NotNull NamespaceDescriptor namespaceDescriptor, @NotNull WritableScope namespaceMemberScope) {
        namespaceMemberScope.importScope(new JavaPackageScope(DescriptorUtils.getFQName(namespaceDescriptor), namespaceDescriptor, javaSemanticServices));
    }
}
