/*
 * Copyright 2010-2012 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.jet.lang.resolve.java;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiModifierListOwner;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.constants.CompileTimeConstant;
import org.jetbrains.jet.lang.resolve.java.JavaDescriptorResolveData.ResolverClassData;
import org.jetbrains.jet.lang.resolve.java.JavaDescriptorResolveData.ResolverScopeData;
import org.jetbrains.jet.lang.resolve.java.resolver.*;
import org.jetbrains.jet.lang.resolve.java.scope.JavaPackageScope;
import org.jetbrains.jet.lang.resolve.java.wrapper.PsiParameterWrapper;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.DependencyClassByQualifiedNameResolver;
import org.jetbrains.jet.lang.types.JetType;

import javax.inject.Inject;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * @author abreslav
 */
public class JavaDescriptorResolver implements DependencyClassByQualifiedNameResolver {

    public static final FqName OBJECT_FQ_NAME = new FqName("java.lang.Object");

    public static final Name JAVA_ROOT = Name.special("<java_root>");

    public static final ModuleDescriptor FAKE_ROOT_MODULE = new ModuleDescriptor(JAVA_ROOT);

    public static Visibility PACKAGE_VISIBILITY = new Visibility("package", false) {
        @Override
        protected boolean isVisible(@NotNull DeclarationDescriptorWithVisibility what, @NotNull DeclarationDescriptor from) {
            NamespaceDescriptor parentPackage = DescriptorUtils.getParentOfType(what, NamespaceDescriptor.class);
            NamespaceDescriptor fromPackage = DescriptorUtils.getParentOfType(from, NamespaceDescriptor.class, false);
            assert parentPackage != null;
            return parentPackage.equals(fromPackage);
        }

        @Override
        protected Integer compareTo(@NotNull Visibility visibility) {
            if (this == visibility) return 0;
            if (visibility == Visibilities.PRIVATE) return 1;
            return -1;
        }
    };

    protected Project project;
    protected JavaSemanticServices semanticServices;
    private BindingTrace trace;
    private PsiClassFinder psiClassFinder;
    private JavaDescriptorSignatureResolver javaDescriptorSignatureResolver;
    private final PropertiesResolver propertiesResolver = new PropertiesResolver(this);
    private final ClassResolver classResolver = new ClassResolver(this);
    private final ConstructorResolver constructorResolver = new ConstructorResolver(this);
    private final CompileTimeConstResolver compileTimeConstResolver = new CompileTimeConstResolver(this);
    private final AnnotationResolver annotationResolver = new AnnotationResolver(this);
    private final FunctionResolver functionResolver = new FunctionResolver(this);
    private final NamespaceResolver namespaceResolver = new NamespaceResolver(this);
    private final InnerClassResolver innerClassResolver = new InnerClassResolver(this);
    private final ValueParameterResolver valueParameterResolver = new ValueParameterResolver(this);

    @Inject
    public void setProject(Project project) {
        this.project = project;
    }

    @Inject
    public void setSemanticServices(JavaSemanticServices semanticServices) {
        this.semanticServices = semanticServices;
        this.propertiesResolver.setSemanticServices(semanticServices);
    }

    @Inject
    public void setTrace(BindingTrace trace) {
        this.trace = trace;
        this.propertiesResolver.setTrace(trace);
    }

    @Inject
    public void setPsiClassFinder(PsiClassFinder psiClassFinder) {
        this.psiClassFinder = psiClassFinder;
    }

    @Inject
    public void setJavaDescriptorSignatureResolver(JavaDescriptorSignatureResolver javaDescriptorSignatureResolver) {
        this.javaDescriptorSignatureResolver = javaDescriptorSignatureResolver;
        this.propertiesResolver.setJavaDescriptorSignatureResolver(javaDescriptorSignatureResolver);
    }

    @Nullable
    public ClassDescriptor resolveClass(@NotNull FqName qualifiedName, @NotNull DescriptorSearchRule searchRule) {
        return classResolver.resolveClass(qualifiedName, searchRule);
    }

    @Override
    public ClassDescriptor resolveClass(@NotNull FqName qualifiedName) {
        return classResolver.resolveClass(qualifiedName, DescriptorSearchRule.ERROR_IF_FOUND_IN_KOTLIN);
    }

    public PsiClassFinder getPsiClassFinder() {
        return psiClassFinder;
    }

    public JavaDescriptorSignatureResolver getJavaDescriptorSignatureResolver() {
        return javaDescriptorSignatureResolver;
    }

    public JavaSemanticServices getSemanticServices() {
        return semanticServices;
    }

    public BindingTrace getTrace() {
        return trace;
    }

    @NotNull
    public Collection<ConstructorDescriptor> resolveConstructors(@NotNull ResolverClassData classData) {
        return constructorResolver.resolveConstructors(classData);
    }

    @Nullable
    public NamespaceDescriptor resolveNamespace(@NotNull FqName qualifiedName, @NotNull DescriptorSearchRule searchRule) {
        return namespaceResolver.resolveNamespace(qualifiedName, searchRule);
    }

    @Override
    public NamespaceDescriptor resolveNamespace(@NotNull FqName qualifiedName) {
        return namespaceResolver.resolveNamespace(qualifiedName);
    }

    @Nullable
    public JavaPackageScope getJavaPackageScope(@NotNull FqName fqName, @NotNull NamespaceDescriptor ns) {
        return namespaceResolver.getJavaPackageScope(fqName, ns);
    }

    public Set<VariableDescriptor> resolveFieldGroupByName(Name name, ResolverScopeData data) {
        return propertiesResolver.resolveFieldGroupByName(name, data);
    }

    public Set<VariableDescriptor> resolveFieldGroup(ResolverScopeData data) {
        return propertiesResolver.resolveFieldGroup(data);
    }

    public ClassDescriptor resolveClass(FqName name, DescriptorSearchRule searchRule, List<Runnable> list) {
        return classResolver.resolveClass(name, searchRule, list);
    }

    public CompileTimeConstant getCompileTimeConstFromExpression(
            FqName annotationName,
            Name parameterName,
            PsiAnnotationMemberValue value,
            List<Runnable> taskList
    ) {
        return compileTimeConstResolver.getCompileTimeConstFromExpression(annotationName, parameterName, value, taskList);
    }

    public static class ValueParameterDescriptors {
        private final JetType receiverType;
        private final List<ValueParameterDescriptor> descriptors;

        public ValueParameterDescriptors(@Nullable JetType receiverType, @NotNull List<ValueParameterDescriptor> descriptors) {
            this.receiverType = receiverType;
            this.descriptors = descriptors;
        }

        @Nullable
        public JetType getReceiverType() {
            return receiverType;
        }

        @NotNull
        public List<ValueParameterDescriptor> getDescriptors() {
            return descriptors;
        }
    }

    @NotNull
    public Set<FunctionDescriptor> resolveFunctionGroup(@NotNull Name methodName, @NotNull ResolverScopeData scopeData) {
        return functionResolver.resolveFunctionGroup(methodName, scopeData);
    }

    public ValueParameterDescriptors resolveParameterDescriptors(
            DeclarationDescriptor containingDeclaration,
            List<PsiParameterWrapper> parameters, TypeVariableResolver typeVariableResolver
    ) {
        return valueParameterResolver.resolveParameterDescriptors(containingDeclaration, parameters, typeVariableResolver);
    }

    public List<AnnotationDescriptor> resolveAnnotations(PsiModifierListOwner owner, @NotNull List<Runnable> tasks) {
        return annotationResolver.resolveAnnotations(owner, tasks);
    }

    public List<AnnotationDescriptor> resolveAnnotations(PsiModifierListOwner owner) {
        return annotationResolver.resolveAnnotations(owner);
    }

    @Nullable
    public AnnotationDescriptor resolveAnnotation(PsiAnnotation psiAnnotation, @NotNull List<Runnable> taskList) {
        return annotationResolver.resolveAnnotation(psiAnnotation, taskList);
    }

    public List<FunctionDescriptor> resolveMethods(@NotNull ResolverScopeData scopeData) {
        return functionResolver.resolveMethods(scopeData);
    }

    public List<ClassDescriptor> resolveInnerClasses(DeclarationDescriptor owner, PsiClass psiClass, boolean staticMembers) {
        return innerClassResolver.resolveInnerClasses(owner, psiClass, staticMembers);
    }
}
