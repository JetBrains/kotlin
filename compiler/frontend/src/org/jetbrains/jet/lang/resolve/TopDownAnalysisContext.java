package org.jetbrains.jet.lang.resolve;

import com.google.common.base.Predicate;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.JetSemanticServices;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;

import java.io.PrintStream;
import java.util.Map;
import java.util.Set;

/**
 * @author abreslav
 */
/*package*/ class TopDownAnalysisContext {

    private final ObservableBindingTrace trace;
    private final JetSemanticServices semanticServices;
    private final DescriptorResolver descriptorResolver;

    private final Map<JetClass, MutableClassDescriptor> classes = Maps.newLinkedHashMap();
    private final Map<JetObjectDeclaration, MutableClassDescriptor> objects = Maps.newLinkedHashMap();
    protected final Map<JetNamespace, WritableScope> namespaceScopes = Maps.newHashMap();
    protected final Map<JetNamespace, NamespaceDescriptorImpl> namespaceDescriptors = Maps.newHashMap();

    private final Map<JetDeclaration, JetScope> declaringScopes = Maps.newHashMap();

    private final Map<JetNamedFunction, FunctionDescriptorImpl> functions = Maps.newLinkedHashMap();
    private final Map<JetSecondaryConstructor, ConstructorDescriptor> constructors = Maps.newLinkedHashMap();
    private final Map<JetProperty, PropertyDescriptor> properties = Maps.newLinkedHashMap();
    private final Set<PropertyDescriptor> primaryConstructorParameterProperties = Sets.newHashSet();

    private final Predicate<PsiFile> analyzeCompletely;
    
    private StringBuilder debugOutput;

    private boolean analyzingBootstrapLibrary = false;

    public TopDownAnalysisContext(JetSemanticServices semanticServices, BindingTrace trace, Predicate<PsiFile> analyzeCompletely) {
        this.trace = new ObservableBindingTrace(trace);
        this.semanticServices = semanticServices;
        this.descriptorResolver = semanticServices.getClassDescriptorResolver(trace);
        this.analyzeCompletely = analyzeCompletely;
    }

    public void debug(Object message) {
        if (debugOutput != null) {
            debugOutput.append(message).append("\n");
        }
    }
    
    /*package*/ void enableDebugOutput() {
        if (debugOutput == null) {
            debugOutput = new StringBuilder();
        }
    }
    
    /*package*/ void printDebugOutput(PrintStream out) {
        if (debugOutput != null) {
            out.print(debugOutput);
        }
    }

    public boolean analyzingBootstrapLibrary() {
        return analyzingBootstrapLibrary;
    }

    public void setAnalyzingBootstrapLibrary(boolean analyzingBootstrapLibrary) {
        this.analyzingBootstrapLibrary = analyzingBootstrapLibrary;
    }

    public boolean completeAnalysisNeeded(@NotNull PsiElement element) {
        PsiFile containingFile = element.getContainingFile();
        boolean result = containingFile != null && analyzeCompletely.apply(containingFile);
        if (!result) {
            debug(containingFile);
        }
        return result;
    }

    public ObservableBindingTrace getTrace() {
        return trace;
    }

    public JetSemanticServices getSemanticServices() {
        return semanticServices;
    }

    public DescriptorResolver getDescriptorResolver() {
        return descriptorResolver;
    }

    public Map<JetClass, MutableClassDescriptor> getClasses() {
        return classes;
    }

    public Map<JetObjectDeclaration, MutableClassDescriptor> getObjects() {
        return objects;
    }

    public Map<JetNamespace, WritableScope> getNamespaceScopes() {
        return namespaceScopes;
    }

    public Map<JetNamespace, NamespaceDescriptorImpl> getNamespaceDescriptors() {
        return namespaceDescriptors;
    }

    public Set<PropertyDescriptor> getPrimaryConstructorParameterProperties() {
        return primaryConstructorParameterProperties;
    }

    public Map<JetSecondaryConstructor, ConstructorDescriptor> getConstructors() {
        return constructors;
    }

    public Map<JetProperty, PropertyDescriptor> getProperties() {
        return properties;
    }

    public Map<JetDeclaration, JetScope> getDeclaringScopes() {
        return declaringScopes;
    }

    public Map<JetNamedFunction, FunctionDescriptorImpl> getFunctions() {
        return functions;
    }

}
