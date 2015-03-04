/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.resolve.lazy;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Predicates;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import kotlin.KotlinPackage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.analyzer.AnalyzerPackage;
import org.jetbrains.kotlin.cfg.JetFlowInformationProvider;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.annotations.Annotated;
import org.jetbrains.kotlin.descriptors.annotations.Annotations;
import org.jetbrains.kotlin.di.InjectorForBodyResolve;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.*;
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo;
import org.jetbrains.kotlin.resolve.lazy.descriptors.LazyClassDescriptor;
import org.jetbrains.kotlin.resolve.lazy.descriptors.LazyPackageDescriptor;
import org.jetbrains.kotlin.resolve.scopes.ChainedScope;
import org.jetbrains.kotlin.resolve.scopes.JetScope;
import org.jetbrains.kotlin.storage.ExceptionTracker;
import org.jetbrains.kotlin.storage.StorageManager;
import org.jetbrains.kotlin.types.TypeConstructor;
import org.jetbrains.kotlin.types.TypeUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import static org.jetbrains.kotlin.resolve.bindingContextUtil.BindingContextUtilPackage.getDataFlowInfo;

public abstract class ElementResolver {

    protected final ResolveSession resolveSession;

    protected ElementResolver(@NotNull ResolveSession session) {
        resolveSession = session;
    }

    @NotNull
    public BindingContext getElementAdditionalResolve(@NotNull JetElement jetElement) {
        return elementAdditionalResolve(jetElement, jetElement, BodyResolveMode.FULL);
    }

    @NotNull
    public BindingContext resolveToElement(@NotNull JetElement jetElement) {
        return resolveToElement(jetElement, BodyResolveMode.FULL);
    }

    @NotNull
    protected ProbablyNothingCallableNames probablyNothingCallableNames() {
        return DefaultNothingCallableNames.INSTANCE$;
    }

    @NotNull
    public BindingContext resolveToElement(@NotNull JetElement jetElement, BodyResolveMode bodyResolveMode) {
        @SuppressWarnings("unchecked") JetElement elementOfAdditionalResolve = (JetElement) JetPsiUtil.getTopmostParentOfTypes(
                jetElement,
                JetNamedFunction.class,
                JetClassInitializer.class,
                JetProperty.class,
                JetParameter.class,
                JetDelegationSpecifierList.class,
                JetImportDirective.class,
                JetAnnotationEntry.class,
                JetTypeParameter.class,
                JetTypeConstraint.class,
                JetPackageDirective.class,
                JetCodeFragment.class);

        if (elementOfAdditionalResolve != null && !(elementOfAdditionalResolve instanceof JetParameter)) {
            if (elementOfAdditionalResolve instanceof JetPackageDirective) {
                elementOfAdditionalResolve = jetElement;
            }

            if (bodyResolveMode != BodyResolveMode.FULL) {
                //TODO: do not resolve with filter if whole body resolve cached already
                return elementAdditionalResolve(elementOfAdditionalResolve, jetElement, bodyResolveMode);
            }

            return getElementAdditionalResolve(elementOfAdditionalResolve);
        }

        JetParameter parameter = (JetParameter) elementOfAdditionalResolve;
        if (parameter != null) {
            JetClass klass = PsiTreeUtil.getParentOfType(parameter, JetClass.class);
            if (klass != null && parameter.getParent() == klass.getPrimaryConstructorParameterList()) {
                return getElementAdditionalResolve(klass);
            }

            // Parameters for function literal could be met inside other parameters. We can't make resolveToDescriptors for internal elements.
            jetElement = parameter;
        }

        JetDeclaration declaration = PsiTreeUtil.getParentOfType(jetElement, JetDeclaration.class, false);
        if (declaration != null && !(declaration instanceof JetClassInitializer)) {
            // Activate descriptor resolution
            resolveSession.resolveToDescriptor(declaration);
        }

        return resolveSession.getBindingContext();
    }

    @NotNull
    protected BindingContext elementAdditionalResolve(
            @NotNull JetElement resolveElement,
            @NotNull JetElement contextElement,
            @NotNull BodyResolveMode bodyResolveMode
    ) {
        // All additional resolve should be done to separate trace
        BindingTrace trace = resolveSession.getStorageManager().createSafeTrace(
                new DelegatingBindingTrace(resolveSession.getBindingContext(), "trace to resolve element", resolveElement));

        JetFile file = resolveElement.getContainingJetFile();

        StatementFilter statementFilter;
        if (bodyResolveMode != BodyResolveMode.FULL && resolveElement instanceof JetDeclaration) {
            statementFilter = new PartialBodyResolveFilter(
                    contextElement,
                    (JetDeclaration) resolveElement,
                    probablyNothingCallableNames(),
                    bodyResolveMode == BodyResolveMode.PARTIAL_FOR_COMPLETION);
        }
        else {
            statementFilter = StatementFilter.NONE;
        }

        if (resolveElement instanceof JetNamedFunction) {
            functionAdditionalResolve(resolveSession, (JetNamedFunction) resolveElement, trace, file, statementFilter);
        }
        else if (resolveElement instanceof JetClassInitializer) {
            initializerAdditionalResolve(resolveSession, (JetClassInitializer) resolveElement, trace, file, statementFilter);
        }
        else if (resolveElement instanceof JetProperty) {
            propertyAdditionalResolve(resolveSession, (JetProperty) resolveElement, trace, file, statementFilter);
        }
        else if (resolveElement instanceof JetDelegationSpecifierList) {
            delegationSpecifierAdditionalResolve(resolveSession, (JetDelegationSpecifierList) resolveElement, trace, file);
        }
        else if (resolveElement instanceof JetImportDirective) {
            JetImportDirective importDirective = (JetImportDirective) resolveElement;
            LazyFileScope scope = resolveSession.getScopeProvider().getFileScope(importDirective.getContainingJetFile());
            scope.forceResolveAllImports();
        }
        else if (resolveElement instanceof JetAnnotationEntry) {
            annotationAdditionalResolve(resolveSession, (JetAnnotationEntry) resolveElement);
        }
        else if (resolveElement instanceof JetClass) {
            constructorAdditionalResolve(resolveSession, (JetClass) resolveElement, trace, file, statementFilter);
        }
        else if (resolveElement instanceof JetTypeParameter) {
            typeParameterAdditionalResolve(resolveSession, (JetTypeParameter) resolveElement);
        }
        else if (resolveElement instanceof JetTypeConstraint) {
            typeConstraintAdditionalResolve(resolveSession, (JetTypeConstraint) resolveElement);
        }
        else if (resolveElement instanceof JetCodeFragment) {
            codeFragmentAdditionalResolve(resolveSession, (JetCodeFragment) resolveElement, trace, bodyResolveMode);
        }
        else if (PsiTreeUtil.getParentOfType(resolveElement, JetPackageDirective.class) != null) {
            packageRefAdditionalResolve(resolveSession, trace, resolveElement);
        }
        else {
            assert false : "Invalid type of the topmost parent";
        }

        new JetFlowInformationProvider(resolveElement, trace).checkDeclaration();

        return trace.getBindingContext();
    }

    private static void packageRefAdditionalResolve(ResolveSession resolveSession, BindingTrace trace, JetElement jetElement) {
        if (jetElement instanceof JetSimpleNameExpression) {
            JetPackageDirective header = PsiTreeUtil.getParentOfType(jetElement, JetPackageDirective.class);
            assert header != null;

            JetSimpleNameExpression packageNameExpression = (JetSimpleNameExpression) jetElement;
            if (trace.getBindingContext().get(BindingContext.RESOLUTION_SCOPE, packageNameExpression) == null) {
                JetScope scope = getExpressionMemberScope(resolveSession, packageNameExpression);
                if (scope != null) {
                    trace.record(BindingContext.RESOLUTION_SCOPE, packageNameExpression, scope);
                }
            }

            if (Name.isValidIdentifier(packageNameExpression.getReferencedName())) {
                if (trace.getBindingContext().get(BindingContext.REFERENCE_TARGET, packageNameExpression) == null) {
                    FqName fqName = header.getFqName(packageNameExpression);
                    PackageViewDescriptor packageDescriptor = resolveSession.getModuleDescriptor().getPackage(fqName);
                    assert packageDescriptor != null: "Package descriptor should be present in session for " + fqName;
                    trace.record(BindingContext.REFERENCE_TARGET, packageNameExpression, packageDescriptor);
                }
            }
        }
    }

    private static void typeConstraintAdditionalResolve(KotlinCodeAnalyzer analyzer, JetTypeConstraint jetTypeConstraint) {
        JetDeclaration declaration = PsiTreeUtil.getParentOfType(jetTypeConstraint, JetDeclaration.class);
        DeclarationDescriptor descriptor = analyzer.resolveToDescriptor(declaration);

        assert (descriptor instanceof ClassDescriptor);

        TypeConstructor constructor = ((ClassDescriptor) descriptor).getTypeConstructor();
        for (TypeParameterDescriptor parameterDescriptor : constructor.getParameters()) {
            ForceResolveUtil.forceResolveAllContents(parameterDescriptor);
        }
    }

    private void codeFragmentAdditionalResolve(
            ResolveSession resolveSession,
            JetCodeFragment codeFragment,
            BindingTrace trace,
            BodyResolveMode bodyResolveMode
    ) {
        JetElement codeFragmentExpression = codeFragment.getContentElement();
        if (!(codeFragmentExpression instanceof JetExpression)) return;

        PsiElement contextElement = codeFragment.getContext();

        JetScope scopeForContextElement;
        DataFlowInfo dataFlowInfoForContextElement;

        if (contextElement instanceof JetClassOrObject) {
            LazyClassDescriptor descriptor = (LazyClassDescriptor) resolveSession.resolveToDescriptor((JetClassOrObject) contextElement);

            scopeForContextElement = descriptor.getScopeForMemberDeclarationResolution();
            dataFlowInfoForContextElement = DataFlowInfo.EMPTY;
        }
        else if (contextElement instanceof JetBlockExpression) {
            JetElement newContextElement = KotlinPackage.lastOrNull(((JetBlockExpression) contextElement).getStatements());

            if (!(newContextElement instanceof JetExpression)) return;

            BindingContext contextForElement = resolveToElement((JetElement) contextElement, BodyResolveMode.FULL);

            scopeForContextElement = contextForElement.get(BindingContext.RESOLUTION_SCOPE, ((JetExpression) newContextElement));
            dataFlowInfoForContextElement = getDataFlowInfo(contextForElement, (JetExpression) newContextElement);
        }
        else {
            if (!(contextElement instanceof JetExpression)) return;

            JetExpression contextExpression = (JetExpression) contextElement;
            BindingContext contextForElement = resolveToElement((JetElement) contextElement, bodyResolveMode);

            scopeForContextElement = contextForElement.get(BindingContext.RESOLUTION_SCOPE, contextExpression);
            dataFlowInfoForContextElement = getDataFlowInfo(contextForElement, contextExpression);
        }

        if (scopeForContextElement == null) return;

        JetScope codeFragmentScope = resolveSession.getScopeProvider().getFileScope(codeFragment);
        ChainedScope chainedScope = new ChainedScope(
                scopeForContextElement.getContainingDeclaration(),
                "Scope for resolve code fragment",
                scopeForContextElement,
                codeFragmentScope
        );

        AnalyzerPackage.computeTypeInContext(
                (JetExpression) codeFragmentExpression,
                chainedScope,
                trace,
                dataFlowInfoForContextElement,
                TypeUtils.NO_EXPECTED_TYPE,
                resolveSession.getModuleDescriptor()
        );
    }

    private static void annotationAdditionalResolve(ResolveSession resolveSession, JetAnnotationEntry jetAnnotationEntry) {
        JetModifierList modifierList = PsiTreeUtil.getParentOfType(jetAnnotationEntry, JetModifierList.class);
        JetDeclaration declaration = PsiTreeUtil.getParentOfType(modifierList, JetDeclaration.class);
        if (declaration != null) {
            doResolveAnnotations(resolveSession, getAnnotationsByDeclaration(resolveSession, modifierList, declaration));
        }
        else {
            JetFileAnnotationList fileAnnotationList = PsiTreeUtil.getParentOfType(jetAnnotationEntry, JetFileAnnotationList.class);
            if (fileAnnotationList != null) {
                doResolveAnnotations(resolveSession, resolveSession.getFileAnnotations(fileAnnotationList.getContainingJetFile()));
            }
            if (modifierList != null && modifierList.getParent() instanceof JetFile) {
                doResolveAnnotations(resolveSession, resolveSession.getDanglingAnnotations(modifierList.getContainingJetFile()));
            }
        }
    }

    private static void doResolveAnnotations(ResolveSession resolveSession, Annotations annotations) {
        AnnotationResolver.resolveAnnotationsArguments(annotations, resolveSession.getTrace());
        ForceResolveUtil.forceResolveAllContents(annotations);
    }

    private static Annotations getAnnotationsByDeclaration(
            ResolveSession resolveSession,
            JetModifierList modifierList,
            JetDeclaration declaration
    ) {
        Annotated descriptor = resolveSession.resolveToDescriptor(declaration);
        if (declaration instanceof JetClass) {
            JetClass jetClass = (JetClass) declaration;
            ClassDescriptor classDescriptor = (ClassDescriptor) descriptor;
            if (modifierList == jetClass.getPrimaryConstructorModifierList()) {
                descriptor = classDescriptor.getUnsubstitutedPrimaryConstructor();
                assert descriptor != null : "No constructor found: " + declaration.getText();
            }
            else if (modifierList.getParent() == jetClass.getBody()) {
                if (classDescriptor instanceof LazyClassDescriptor) {
                    return ((LazyClassDescriptor) classDescriptor).getDanglingAnnotations();
                }
            }
        }
        return descriptor.getAnnotations();
    }

    private static void typeParameterAdditionalResolve(KotlinCodeAnalyzer analyzer, JetTypeParameter typeParameter) {
        DeclarationDescriptor descriptor = analyzer.resolveToDescriptor(typeParameter);
        ForceResolveUtil.forceResolveAllContents(descriptor);
    }

    private void delegationSpecifierAdditionalResolve(
            ResolveSession resolveSession,
            JetDelegationSpecifierList specifier, BindingTrace trace, JetFile file) {

        JetClassOrObject classOrObject = (JetClassOrObject) specifier.getParent();
        LazyClassDescriptor descriptor = (LazyClassDescriptor) resolveSession.resolveToDescriptor(classOrObject);

        // Activate resolving of supertypes
        ForceResolveUtil.forceResolveAllContents(descriptor.getTypeConstructor().getSupertypes());

        BodyResolver bodyResolver = createBodyResolver(resolveSession, trace, file, StatementFilter.NONE);
        bodyResolver.resolveDelegationSpecifierList(createEmptyContext(resolveSession), classOrObject, descriptor,
                                                    descriptor.getUnsubstitutedPrimaryConstructor(),
                                                    descriptor.getScopeForClassHeaderResolution(),
                                                    descriptor.getScopeForMemberDeclarationResolution());
    }

    private void propertyAdditionalResolve(
            final ResolveSession resolveSession,
            final JetProperty jetProperty,
            BindingTrace trace,
            JetFile file,
            @NotNull StatementFilter statementFilter
    ) {
        JetScope propertyResolutionScope = resolveSession.getScopeProvider().getResolutionScopeForDeclaration(jetProperty);

        BodyResolveContextForLazy bodyResolveContext = new BodyResolveContextForLazy(
                createParameters(resolveSession),
                new Function<JetDeclaration, JetScope>() {
                    @Override
                    public JetScope apply(JetDeclaration declaration) {
                        assert declaration.getParent() == jetProperty : "Must be called only for property accessors, but called for " +
                                                                        declaration;
                        return resolveSession.getScopeProvider().getResolutionScopeForDeclaration(declaration);
                    }
                });
        BodyResolver bodyResolver = createBodyResolver(resolveSession, trace, file, statementFilter);
        PropertyDescriptor descriptor = (PropertyDescriptor) resolveSession.resolveToDescriptor(jetProperty);
        ForceResolveUtil.forceResolveAllContents(descriptor);

        JetExpression propertyInitializer = jetProperty.getInitializer();
        if (propertyInitializer != null) {
            bodyResolver.resolvePropertyInitializer(bodyResolveContext, jetProperty, descriptor, propertyInitializer, propertyResolutionScope);
        }

        JetExpression propertyDelegate = jetProperty.getDelegateExpression();
        if (propertyDelegate != null) {
            bodyResolver.resolvePropertyDelegate(bodyResolveContext, jetProperty, descriptor, propertyDelegate, propertyResolutionScope, propertyResolutionScope);
        }

        bodyResolver.resolvePropertyAccessors(bodyResolveContext, jetProperty, descriptor);

        for (JetPropertyAccessor accessor : jetProperty.getAccessors()) {
            new JetFlowInformationProvider(accessor, trace).checkDeclaration();
        }
    }

    private void functionAdditionalResolve(
            ResolveSession resolveSession,
            JetNamedFunction namedFunction,
            BindingTrace trace,
            JetFile file,
            @NotNull StatementFilter statementFilter
    ) {
        JetScope scope = resolveSession.getScopeProvider().getResolutionScopeForDeclaration(namedFunction);
        FunctionDescriptor functionDescriptor = (FunctionDescriptor) resolveSession.resolveToDescriptor(namedFunction);
        ForceResolveUtil.forceResolveAllContents(functionDescriptor);

        BodyResolver bodyResolver = createBodyResolver(resolveSession, trace, file, statementFilter);
        bodyResolver.resolveFunctionBody(createEmptyContext(resolveSession), trace, namedFunction, functionDescriptor, scope);
    }

    private void constructorAdditionalResolve(
            ResolveSession resolveSession,
            JetClass klass,
            BindingTrace trace,
            JetFile file,
            @NotNull StatementFilter statementFilter
    ) {
        JetScope scope = resolveSession.getScopeProvider().getResolutionScopeForDeclaration(klass);

        ClassDescriptor classDescriptor = (ClassDescriptor) resolveSession.resolveToDescriptor(klass);
        ConstructorDescriptor constructorDescriptor = classDescriptor.getUnsubstitutedPrimaryConstructor();
        assert constructorDescriptor != null :
                String.format("Can't get primary constructor for descriptor '%s' in from class '%s'",
                              classDescriptor,
                              JetPsiUtil.getElementTextWithContext(klass));

        BodyResolver bodyResolver = createBodyResolver(resolveSession, trace, file, statementFilter);
        bodyResolver.resolveConstructorParameterDefaultValuesAndAnnotations(createEmptyContext(resolveSession), trace, klass,
                                                                            constructorDescriptor, scope);
    }

    private void initializerAdditionalResolve(
            ResolveSession resolveSession,
            JetClassInitializer classInitializer,
            BindingTrace trace,
            JetFile file,
            @NotNull StatementFilter statementFilter
    ) {
        JetClassOrObject classOrObject = PsiTreeUtil.getParentOfType(classInitializer, JetClassOrObject.class);
        LazyClassDescriptor classOrObjectDescriptor = (LazyClassDescriptor) resolveSession.resolveToDescriptor(classOrObject);

        BodyResolver bodyResolver = createBodyResolver(resolveSession, trace, file, statementFilter);
        bodyResolver.resolveAnonymousInitializer(createEmptyContext(resolveSession), classInitializer, classOrObjectDescriptor);
    }

    private BodyResolver createBodyResolver(ResolveSession resolveSession, BindingTrace trace, JetFile file, @NotNull StatementFilter statementFilter) {
        InjectorForBodyResolve bodyResolve = new InjectorForBodyResolve(
                file.getProject(),
                createParameters(resolveSession),
                trace,
                resolveSession.getModuleDescriptor(),
                getAdditionalCheckerProvider(file),
                statementFilter
        );
        return bodyResolve.getBodyResolver();
    }

    private static TopDownAnalysisParameters createParameters(@NotNull ResolveSession resolveSession) {
        return TopDownAnalysisParameters.createForLocalDeclarations(
                resolveSession.getStorageManager(), resolveSession.getExceptionTracker(),
                Predicates.<PsiFile>alwaysTrue());
    }

    @NotNull
    private static BodyResolveContextForLazy createEmptyContext(@NotNull ResolveSession resolveSession) {
        return new BodyResolveContextForLazy(createParameters(resolveSession), Functions.<JetScope>constant(null));
    }

    private static JetScope getExpressionResolutionScope(@NotNull ResolveSession resolveSession, @NotNull JetExpression expression) {
        ScopeProvider provider = resolveSession.getScopeProvider();
        JetDeclaration parentDeclaration = PsiTreeUtil.getParentOfType(expression, JetDeclaration.class);
        if (parentDeclaration == null) {
            return provider.getFileScope(expression.getContainingJetFile());
        }
        return provider.getResolutionScopeForDeclaration(parentDeclaration);
    }

    private static JetScope getExpressionMemberScope(@NotNull ResolveSession resolveSession, @NotNull JetExpression expression) {
        BindingTrace trace = resolveSession.getStorageManager().createSafeTrace(new DelegatingBindingTrace(
                resolveSession.getBindingContext(), "trace to resolve a member scope of expression", expression));

        if (BindingContextUtils.isExpressionWithValidReference(expression, resolveSession.getBindingContext())) {
            QualifiedExpressionResolver qualifiedExpressionResolver = resolveSession.getQualifiedExpressionResolver();

            // In some type declaration
            if (expression.getParent() instanceof JetUserType) {
                JetUserType qualifier = ((JetUserType) expression.getParent()).getQualifier();
                if (qualifier != null) {
                    JetScope resolutionScope = getExpressionResolutionScope(resolveSession, expression);
                    Collection<DeclarationDescriptor> descriptors =
                            qualifiedExpressionResolver.lookupDescriptorsForUserType(qualifier, resolutionScope, trace, false);
                    for (DeclarationDescriptor descriptor : descriptors) {
                        if (descriptor instanceof LazyPackageDescriptor) {
                            return ((LazyPackageDescriptor) descriptor).getMemberScope();
                        }
                    }
                }
            }

            // Inside import
            if (PsiTreeUtil.getParentOfType(expression, JetImportDirective.class, false) != null) {
                PackageViewDescriptor rootPackage = resolveSession.getModuleDescriptor().getPackage(FqName.ROOT);
                assert rootPackage != null;

                if (expression.getParent() instanceof JetDotQualifiedExpression) {
                    JetExpression element = ((JetDotQualifiedExpression) expression.getParent()).getReceiverExpression();
                    FqName fqName = expression.getContainingJetFile().getPackageFqName();

                    PackageViewDescriptor filePackage = resolveSession.getModuleDescriptor().getPackage(fqName);
                    assert filePackage != null : "File package should be already resolved and be found";

                    JetScope scope = filePackage.getMemberScope();
                    Collection<? extends DeclarationDescriptor> descriptors;

                    if (element instanceof JetDotQualifiedExpression) {
                        descriptors = qualifiedExpressionResolver.lookupDescriptorsForQualifiedExpression(
                                (JetDotQualifiedExpression) element, rootPackage.getMemberScope(), scope, trace,
                                QualifiedExpressionResolver.LookupMode.EVERYTHING, false);
                    }
                    else {
                        descriptors = qualifiedExpressionResolver.lookupDescriptorsForSimpleNameReference(
                                (JetSimpleNameExpression) element, rootPackage.getMemberScope(), scope, trace,
                                QualifiedExpressionResolver.LookupMode.EVERYTHING, false, false);
                    }

                    for (DeclarationDescriptor descriptor : descriptors) {
                        if (descriptor instanceof PackageViewDescriptor) {
                            return ((PackageViewDescriptor) descriptor).getMemberScope();
                        }
                    }
                }
                else {
                    return rootPackage.getMemberScope();
                }
            }

            // Inside package declaration
            JetPackageDirective packageDirective = PsiTreeUtil.getParentOfType(expression, JetPackageDirective.class, false);
            if (packageDirective != null) {
                PackageViewDescriptor packageDescriptor = resolveSession.getModuleDescriptor().getPackage(
                        packageDirective.getFqName((JetSimpleNameExpression) expression).parent());
                if (packageDescriptor != null) {
                    return packageDescriptor.getMemberScope();
                }
            }
        }

        return null;
    }

    @NotNull
    protected abstract AdditionalCheckerProvider getAdditionalCheckerProvider(@NotNull JetFile jetFile);

    private static class BodyResolveContextForLazy implements BodiesResolveContext {

        private final Function<? super JetDeclaration, JetScope> declaringScopes;
        private final TopDownAnalysisParameters topDownAnalysisParameters;

        private BodyResolveContextForLazy(
                @NotNull TopDownAnalysisParameters parameters,
                @NotNull Function<? super JetDeclaration, JetScope> declaringScopes
        ) {
            this.topDownAnalysisParameters = parameters;
            this.declaringScopes = declaringScopes;
        }

        @NotNull
        @Override
        public StorageManager getStorageManager() {
            return topDownAnalysisParameters.getStorageManager();
        }

        @NotNull
        @Override
        public ExceptionTracker getExceptionTracker() {
            return topDownAnalysisParameters.getExceptionTracker();
        }

        @Override
        public Collection<JetFile> getFiles() {
            return Collections.emptySet();
        }

        @Override
        public Map<JetClassOrObject, ClassDescriptorWithResolutionScopes> getDeclaredClasses() {
            return Collections.emptyMap();
        }

        @Override
        public Map<JetClassInitializer, ClassDescriptorWithResolutionScopes> getAnonymousInitializers() {
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
        public Function<JetDeclaration, JetScope> getDeclaringScopes() {
            //noinspection unchecked
            return (Function<JetDeclaration, JetScope>) declaringScopes;
        }

        @Override
        public Map<JetScript, ScriptDescriptor> getScripts() {
            return Collections.emptyMap();
        }

        @Override
        public DataFlowInfo getOuterDataFlowInfo() {
            return DataFlowInfo.EMPTY;
        }

        @NotNull
        @Override
        public TopDownAnalysisParameters getTopDownAnalysisParameters() {
            return topDownAnalysisParameters;
        }

        @Override
        public boolean completeAnalysisNeeded(@NotNull PsiElement element) {
            return true;
        }
    }
}
