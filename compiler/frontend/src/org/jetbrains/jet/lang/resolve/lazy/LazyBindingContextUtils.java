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

package org.jetbrains.jet.lang.resolve.lazy;

import com.google.common.base.Predicates;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.di.InjectorForBodyResolve;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.*;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * @author Nikolay Krasko
 */
public class LazyBindingContextUtils {
    private static final Logger LOG = Logger.getInstance("#" + LazyBindingContextUtils.class.getName());

    private LazyBindingContextUtils() {
    }

    private static class EmptyBodyResolveContext implements BodiesResolveContext {
        @Override
        public Map<JetClass, MutableClassDescriptor> getClasses() {
            return Collections.emptyMap();
        }

        @Override
        public Map<JetObjectDeclaration, MutableClassDescriptor> getObjects() {
            return Collections.emptyMap();
        }

        @Override
        public Map<JetProperty, PropertyDescriptor> getProperties() {
            return Collections.emptyMap();
        }

        @Override
        public Map<JetNamedFunction, SimpleFunctionDescriptor> getFunctions() {
            return Collections.emptyMap();
        }

        @Override
        public Map<JetDeclaration, JetScope> getDeclaringScopes() {
            return Collections.emptyMap();
        }

        @Override
        public Map<JetScript, ScriptDescriptor> getScripts() {
            return Collections.emptyMap();
        }

        @Override
        public Map<JetScript, WritableScope> getScriptScopes() {
            return Collections.emptyMap();
        }

        @Override
        public void setTopDownAnalysisParameters(TopDownAnalysisParameters parameters) {
        }

        @Override
        public boolean completeAnalysisNeeded(@NotNull PsiElement element) {
            return true;
        }
    }

    public static @NotNull BindingContext getExpressionBindingContext(@NotNull ResolveSession resolveSession, @NotNull JetExpression expression) {
        final DelegatingBindingTrace trace = new DelegatingBindingTrace(resolveSession.getBindingContext());

        JetFile file = (JetFile) expression.getContainingFile();

        // Resolve enclosing function body
        JetNamedFunction namedFunction = PsiTreeUtil.getTopmostParentOfType(expression, JetNamedFunction.class);
        if (namedFunction != null) {
            TopDownAnalysisParameters parameters = new TopDownAnalysisParameters(
                    Predicates.<PsiFile>alwaysTrue(), false, true, Collections.<AnalyzerScriptParameter>emptyList());

            InjectorForBodyResolve bodyResolve = new InjectorForBodyResolve(file.getProject(), parameters, trace, new EmptyBodyResolveContext());

            final BodyResolver bodyResolver = bodyResolve.getBodyResolver();

            if (namedFunction.getParent() instanceof JetFile ||
                    namedFunction.getParent() instanceof JetClassBody ||
                    namedFunction.getParent() instanceof JetClassObject) {
                JetScope scope = resolveSession.getResolutionScope(namedFunction);
                FunctionDescriptor functionDescriptor = (FunctionDescriptor) resolveSession.resolveToDescriptor(namedFunction);
                bodyResolver.resolveFunctionBody(trace, namedFunction, functionDescriptor, scope);
            }
            else {
                LOG.warn(String.format("Not expected parent for function: %s in %s",
                        namedFunction.getName(), namedFunction.getContainingFile()));
            }
        }

        if (trace.getBindingContext().get(BindingContext.RESOLUTION_SCOPE, expression) == null) {
            JetScope scope = getExpressionMemberScope(resolveSession, expression);
            if (scope != null) {
                trace.record(BindingContext.RESOLUTION_SCOPE, expression, scope);
            }
        }

        return trace.getBindingContext();
    }

    private static JetScope getExpressionOuterScope(@NotNull ResolveSession resolveSession, @NotNull JetExpression expression) {
        JetDeclaration parentDeclaration = PsiTreeUtil.getParentOfType(expression, JetDeclaration.class);
        // DeclarationDescriptor descriptor = resolveToDescriptor(parentDeclaration);
        return resolveSession.getResolutionScope(parentDeclaration);
    }

    public static JetScope getExpressionMemberScope(@NotNull ResolveSession resolveSession, @NotNull JetExpression expression) {
        DelegatingBindingTrace trace = new DelegatingBindingTrace(resolveSession.getBindingContext());

        if (expression instanceof JetReferenceExpression) {
            // In some type declaration
            if (expression.getParent() instanceof JetUserType) {
                JetUserType qualifier = ((JetUserType) expression.getParent()).getQualifier();
                if (qualifier != null) {
                    Collection<? extends DeclarationDescriptor> descriptors = resolveSession.getInjector().getQualifiedExpressionResolver()
                            .lookupDescriptorsForUserType(qualifier, getExpressionOuterScope(resolveSession, expression), trace);

                    for (DeclarationDescriptor descriptor : descriptors) {
                        if (descriptor instanceof LazyPackageDescriptor) {
                            return ((LazyPackageDescriptor) descriptor).getMemberScope();
                        }

                    }
                }
            }

            // Inside import
            if (PsiTreeUtil.getParentOfType(expression, JetImportDirective.class, false) != null) {
                if (expression.getParent() instanceof JetDotQualifiedExpression) {
                    JetExpression element = ((JetDotQualifiedExpression) expression.getParent()).getReceiverExpression();

                    NamespaceDescriptor filePackage = resolveSession.getPackageDescriptorByFqName(
                            new FqName(((JetFile) expression.getContainingFile()).getPackageName()));

                    NamespaceDescriptor rootPackage = resolveSession.getPackageDescriptorByFqName(FqName.ROOT);

                    Collection<? extends DeclarationDescriptor> descriptors;

                    if (element instanceof JetDotQualifiedExpression) {
                        descriptors = resolveSession.getInjector().getQualifiedExpressionResolver().lookupDescriptorsForQualifiedExpression(
                                (JetDotQualifiedExpression) element, rootPackage.getMemberScope(), filePackage.getMemberScope(), trace, false, false);
                    }
                    else {
                        descriptors = resolveSession.getInjector().getQualifiedExpressionResolver().lookupDescriptorsForSimpleNameReference(
                                (JetSimpleNameExpression) element, rootPackage.getMemberScope(), filePackage.getMemberScope(), trace, false, false, false);
                    }

                    for (DeclarationDescriptor descriptor : descriptors) {
                        if (descriptor instanceof NamespaceDescriptor) {
                            return ((NamespaceDescriptor) descriptor).getMemberScope();
                        }
                    }
                }
                else {
                    return resolveSession.getPackageDescriptorByFqName(FqName.ROOT).getMemberScope();
                }
            }

            // Inside package declaration
            JetNamespaceHeader namespaceHeader = PsiTreeUtil.getParentOfType(expression, JetNamespaceHeader.class, false);
            if (namespaceHeader != null) {
                NamespaceDescriptor packageDescriptor = resolveSession.getPackageDescriptorByFqName(
                        namespaceHeader.getParentFqName((JetReferenceExpression)expression));
                if (packageDescriptor != null) {
                    return packageDescriptor.getMemberScope();
                }
            }
        }

        return null;
    }
}
