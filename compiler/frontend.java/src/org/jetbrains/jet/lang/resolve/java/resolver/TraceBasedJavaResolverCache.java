/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.resolve.java.resolver;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiField;
import com.intellij.psi.impl.JavaConstantExpressionEvaluator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.resolve.AnnotationUtils;
import org.jetbrains.jet.lang.resolve.BindingContextUtils;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.constants.CompileTimeConstant;
import org.jetbrains.jet.lang.resolve.java.JavaBindingContext;
import org.jetbrains.jet.lang.resolve.java.JavaNamespaceKind;
import org.jetbrains.jet.lang.resolve.java.structure.JavaClass;
import org.jetbrains.jet.lang.resolve.java.structure.JavaElement;
import org.jetbrains.jet.lang.resolve.java.structure.JavaField;
import org.jetbrains.jet.lang.resolve.java.structure.JavaMethod;
import org.jetbrains.jet.lang.resolve.java.structure.impl.JavaClassImpl;
import org.jetbrains.jet.lang.resolve.java.structure.impl.JavaElementImpl;
import org.jetbrains.jet.lang.resolve.java.structure.impl.JavaFieldImpl;
import org.jetbrains.jet.lang.resolve.java.structure.impl.JavaMethodImpl;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.rt.annotation.AssertInvisibleInResolver;

import javax.inject.Inject;

import static org.jetbrains.jet.lang.resolve.BindingContext.*;
import static org.jetbrains.jet.lang.resolve.java.resolver.DescriptorResolverUtils.fqNameByClass;

public class TraceBasedJavaResolverCache implements JavaResolverCache {
    private static final Logger LOG = Logger.getInstance(TraceBasedJavaResolverCache.class);
    private static final FqName ASSERT_INVISIBLE_IN_RESOLVER_ANNOTATION = fqNameByClass(AssertInvisibleInResolver.class);

    private BindingTrace trace;

    @Inject
    public void setTrace(BindingTrace trace) {
        this.trace = trace;
    }

    @Nullable
    @Override
    public ClassDescriptor getClassResolvedFromSource(@NotNull FqName fqName) {
        return trace.get(FQNAME_TO_CLASS_DESCRIPTOR, fqName);
    }

    @Nullable
    @Override
    public SimpleFunctionDescriptor getMethod(@NotNull JavaMethod method) {
        return trace.get(FUNCTION, ((JavaMethodImpl) method).getPsi());
    }

    @Nullable
    @Override
    public ConstructorDescriptor getConstructor(@NotNull JavaElement constructor) {
        return trace.get(CONSTRUCTOR, ((JavaElementImpl) constructor).getPsi());
    }

    @Nullable
    @Override
    public ClassDescriptor getClass(@NotNull JavaClass javaClass) {
        FqName fqName = javaClass.getFqName();
        if (fqName != null && KotlinBuiltIns.BUILT_INS_PACKAGE_FQ_NAME.equals(fqName.parent())) {
            if (javaClass.findAnnotation(ASSERT_INVISIBLE_IN_RESOLVER_ANNOTATION) != null) {
                if (ApplicationManager.getApplication().isInternal()) {
                    LOG.error("Classpath is configured incorrectly:" +
                              " class " + fqName + " from runtime must not be loaded by compiler");
                }
                return null;
            }
        }

        return trace.get(CLASS, ((JavaClassImpl) javaClass).getPsi());
    }

    @Override
    public void recordMethod(@NotNull JavaMethod method, @NotNull SimpleFunctionDescriptor descriptor) {
        BindingContextUtils.recordFunctionDeclarationToDescriptor(trace, ((JavaMethodImpl) method).getPsi(), descriptor);
    }

    @Override
    public void recordConstructor(@NotNull JavaElement element, @NotNull ConstructorDescriptor descriptor) {
        trace.record(CONSTRUCTOR, ((JavaElementImpl) element).getPsi(), descriptor);
    }

    @Override
    public void recordField(@NotNull JavaField field, @NotNull PropertyDescriptor descriptor) {
        PsiField psiField = ((JavaFieldImpl) field).getPsi();
        trace.record(VARIABLE, psiField, descriptor);

        if (AnnotationUtils.isPropertyCompileTimeConstant(descriptor)) {
            PsiExpression initializer = psiField.getInitializer();
            Object evaluatedExpression = JavaConstantExpressionEvaluator.computeConstantExpression(initializer, false);
            if (evaluatedExpression != null) {
                CompileTimeConstant<?> constant = JavaAnnotationArgumentResolver.resolveCompileTimeConstantValue(evaluatedExpression, descriptor.getType());
                if (constant != null) {
                    trace.record(COMPILE_TIME_INITIALIZER, descriptor, constant);
                }
            }
        }
    }

    @Override
    public void recordClass(@NotNull JavaClass javaClass, @NotNull ClassDescriptor descriptor) {
        trace.record(CLASS, ((JavaClassImpl) javaClass).getPsi(), descriptor);
    }

    @Override
    public void recordProperPackage(@NotNull PackageFragmentDescriptor descriptor) {
        trace.record(JavaBindingContext.JAVA_NAMESPACE_KIND, descriptor, JavaNamespaceKind.PROPER);
    }

    @Override
    public void recordClassStaticMembersNamespace(@NotNull PackageFragmentDescriptor descriptor) {
        trace.record(JavaBindingContext.JAVA_NAMESPACE_KIND, descriptor, JavaNamespaceKind.CLASS_STATICS);
    }
}
