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

package org.jetbrains.jet.lang.resolve;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.*;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiElement;
import com.intellij.util.containers.ContainerUtil;
import kotlin.Function1;
import kotlin.KotlinPackage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.impl.MutableClassDescriptor;
import org.jetbrains.jet.lang.descriptors.impl.MutablePackageFragmentDescriptor;
import org.jetbrains.jet.lang.descriptors.impl.PackageLikeBuilder;
import org.jetbrains.jet.lang.diagnostics.Errors;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.lazy.KotlinCodeAnalyzer;
import org.jetbrains.jet.lang.resolve.lazy.descriptors.LazyPackageDescriptor;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.renderer.DescriptorRenderer;

import javax.inject.Inject;
import java.util.*;

import static org.jetbrains.jet.lang.diagnostics.Errors.REDECLARATION;
import static org.jetbrains.jet.lang.resolve.ScriptHeaderResolver.resolveScriptDeclarations;

public class DeclarationResolver {
    @NotNull
    private AnnotationResolver annotationResolver;
    @NotNull
    private ImportsResolver importsResolver;
    @NotNull
    private DescriptorResolver descriptorResolver;
    @NotNull
    private BindingTrace trace;


    @Inject
    public void setAnnotationResolver(@NotNull AnnotationResolver annotationResolver) {
        this.annotationResolver = annotationResolver;
    }

    @Inject
    public void setImportsResolver(@NotNull ImportsResolver importsResolver) {
        this.importsResolver = importsResolver;
    }

    @Inject
    public void setDescriptorResolver(@NotNull DescriptorResolver descriptorResolver) {
        this.descriptorResolver = descriptorResolver;
    }

    @Inject
    public void setTrace(@NotNull BindingTrace trace) {
        this.trace = trace;
    }


    public void process(@NotNull TopDownAnalysisContext c) {
        resolveAnnotationConstructors(c);
        resolveConstructorHeaders(c);
        resolveAnnotationStubsOnClassesAndConstructors(c);
        resolveFunctionAndPropertyHeaders(c);

        // SCRIPT: Resolve script declarations
        resolveScriptDeclarations(c);

        createFunctionsForDataClasses(c);
        importsResolver.processMembersImports(c);
        checkRedeclarationsInPackages(c);
        checkRedeclarationsInInnerClassNames(c);
    }

    private void resolveAnnotationConstructors(@NotNull TopDownAnalysisContext c) {
        for (Map.Entry<JetClassOrObject, ClassDescriptorWithResolutionScopes> entry : c.getDeclaredClasses().entrySet()) {
            JetClassOrObject classOrObject = entry.getKey();
            MutableClassDescriptor classDescriptor = (MutableClassDescriptor) entry.getValue();

            if (classOrObject instanceof JetClass && DescriptorUtils.isAnnotationClass(classDescriptor)) {
                processPrimaryConstructor(c, classDescriptor, (JetClass) classOrObject);
            }
        }
    }

    private void resolveConstructorHeaders(@NotNull TopDownAnalysisContext c) {
        for (Map.Entry<JetClassOrObject, ClassDescriptorWithResolutionScopes> entry : c.getDeclaredClasses().entrySet()) {
            JetClassOrObject classOrObject = entry.getKey();
            MutableClassDescriptor classDescriptor = (MutableClassDescriptor) entry.getValue();

            if (classOrObject instanceof JetClass && !DescriptorUtils.isAnnotationClass(classDescriptor)) {
                processPrimaryConstructor(c, classDescriptor, (JetClass) classOrObject);
            }
        }
    }

    private void resolveAnnotationStubsOnClassesAndConstructors(@NotNull TopDownAnalysisContext c) {
        for (Map.Entry<JetClassOrObject, ClassDescriptorWithResolutionScopes> entry : c.getDeclaredClasses().entrySet()) {
            JetModifierList modifierList = entry.getKey().getModifierList();
            if (modifierList != null) {
                MutableClassDescriptor descriptor = (MutableClassDescriptor) entry.getValue();
                descriptor.addAnnotations(annotationResolver.resolveAnnotationsWithoutArguments(
                        descriptor.getScopeForClassHeaderResolution(), modifierList, trace));
            }
        }
    }

    private void resolveFunctionAndPropertyHeaders(@NotNull TopDownAnalysisContext c) {
        for (Map.Entry<JetFile, WritableScope> entry : c.getFileScopes().entrySet()) {
            JetFile file = entry.getKey();
            WritableScope fileScope = entry.getValue();
            PackageLikeBuilder packageBuilder = c.getPackageFragments().get(file).getBuilder();

            resolveFunctionAndPropertyHeaders(c, file.getDeclarations(), fileScope, fileScope, fileScope, packageBuilder);
        }
        for (Map.Entry<JetClassOrObject, ClassDescriptorWithResolutionScopes> entry : c.getDeclaredClasses().entrySet()) {
            JetClassOrObject classOrObject = entry.getKey();
            MutableClassDescriptor classDescriptor = (MutableClassDescriptor) entry.getValue();

            resolveFunctionAndPropertyHeaders(
                    c,
                    classOrObject.getDeclarations(), classDescriptor.getScopeForMemberDeclarationResolution(),
                    classDescriptor.getScopeForInitializerResolution(), classDescriptor.getScopeForMemberDeclarationResolution(),
                    classDescriptor.getBuilder());
        }

        // TODO : Extensions
    }

    private void resolveFunctionAndPropertyHeaders(
            @NotNull final TopDownAnalysisContext c,
            @NotNull List<JetDeclaration> declarations,
            @NotNull final JetScope scopeForFunctions,
            @NotNull final JetScope scopeForPropertyInitializers,
            @NotNull final JetScope scopeForPropertyAccessors,
            @NotNull final PackageLikeBuilder packageLike)
    {
        for (JetDeclaration declaration : declarations) {
            declaration.accept(new JetVisitorVoid() {
                @Override
                public void visitNamedFunction(@NotNull JetNamedFunction function) {
                    SimpleFunctionDescriptor functionDescriptor = descriptorResolver.resolveFunctionDescriptor(
                            packageLike.getOwnerForChildren(),
                            scopeForFunctions,
                            function,
                            trace,
                            c.getOuterDataFlowInfo()
                    );
                    packageLike.addFunctionDescriptor(functionDescriptor);
                    c.getFunctions().put(function, functionDescriptor);
                    c.registerDeclaringScope(function, scopeForFunctions);
                }

                @Override
                public void visitProperty(@NotNull JetProperty property) {
                    PropertyDescriptor propertyDescriptor = descriptorResolver.resolvePropertyDescriptor(
                            packageLike.getOwnerForChildren(),
                            scopeForPropertyInitializers,
                            property,
                            trace,
                            c.getOuterDataFlowInfo());
                    packageLike.addPropertyDescriptor(propertyDescriptor);
                    c.getProperties().put(property, propertyDescriptor);
                    c.registerDeclaringScope(property, scopeForPropertyInitializers);
                    JetPropertyAccessor getter = property.getGetter();
                    if (getter != null) {
                        c.registerDeclaringScope(getter, scopeForPropertyAccessors);
                    }
                    JetPropertyAccessor setter = property.getSetter();
                    if (setter != null) {
                        c.registerDeclaringScope(setter, scopeForPropertyAccessors);
                    }
                }
            });
        }
    }

    private void createFunctionsForDataClasses(@NotNull TopDownAnalysisContext c) {
        for (Map.Entry<JetClassOrObject, ClassDescriptorWithResolutionScopes> entry : c.getDeclaredClasses().entrySet()) {
            JetClassOrObject klass = entry.getKey();
            MutableClassDescriptor classDescriptor = (MutableClassDescriptor) entry.getValue();

            if (klass instanceof JetClass && klass.hasPrimaryConstructor() && KotlinBuiltIns.getInstance().isData(classDescriptor)) {
                ConstructorDescriptor constructor = getConstructorOfDataClass(classDescriptor);
                createComponentFunctions(classDescriptor, constructor);
                createCopyFunction(classDescriptor, constructor);
            }
        }
    }

    @NotNull
    public static ConstructorDescriptor getConstructorOfDataClass(@NotNull ClassDescriptor classDescriptor) {
        Collection<ConstructorDescriptor> constructors = classDescriptor.getConstructors();
        assert constructors.size() == 1 : "Data class must have only one constructor: " + classDescriptor.getConstructors();
        return constructors.iterator().next();
    }

    private void createComponentFunctions(@NotNull MutableClassDescriptor classDescriptor, @NotNull ConstructorDescriptor constructorDescriptor) {
        int parameterIndex = 0;
        for (ValueParameterDescriptor parameter : constructorDescriptor.getValueParameters()) {
            if (!parameter.getType().isError()) {
                PropertyDescriptor property = trace.get(BindingContext.VALUE_PARAMETER_AS_PROPERTY, parameter);
                if (property != null) {
                    ++parameterIndex;

                    SimpleFunctionDescriptor functionDescriptor =
                            DescriptorResolver.createComponentFunctionDescriptor(parameterIndex, property, parameter, classDescriptor, trace);

                    classDescriptor.getBuilder().addFunctionDescriptor(functionDescriptor);
                }
            }
        }
    }

    private void createCopyFunction(@NotNull MutableClassDescriptor classDescriptor, @NotNull ConstructorDescriptor constructorDescriptor) {
        SimpleFunctionDescriptor functionDescriptor = DescriptorResolver.createCopyFunctionDescriptor(
                constructorDescriptor.getValueParameters(), classDescriptor, trace);

        classDescriptor.getBuilder().addFunctionDescriptor(functionDescriptor);
    }

    private void processPrimaryConstructor(
            @NotNull TopDownAnalysisContext c,
            @NotNull MutableClassDescriptor classDescriptor,
            @NotNull JetClass klass
    ) {
        // TODO : not all the parameters are real properties
        JetScope memberScope = classDescriptor.getScopeForClassHeaderResolution();
        ConstructorDescriptor constructorDescriptor = descriptorResolver.resolvePrimaryConstructorDescriptor(memberScope, classDescriptor, klass, trace);
        if (constructorDescriptor != null) {
            List<ValueParameterDescriptor> valueParameterDescriptors = constructorDescriptor.getValueParameters();
            List<JetParameter> primaryConstructorParameters = klass.getPrimaryConstructorParameters();
            assert valueParameterDescriptors.size() == primaryConstructorParameters.size();
            List<ValueParameterDescriptor> notProperties = new ArrayList<ValueParameterDescriptor>();
            for (ValueParameterDescriptor valueParameterDescriptor : valueParameterDescriptors) {
                JetParameter parameter = primaryConstructorParameters.get(valueParameterDescriptor.getIndex());
                if (parameter.hasValOrVarNode()) {
                    PropertyDescriptor propertyDescriptor = descriptorResolver.resolvePrimaryConstructorParameterToAProperty(
                            classDescriptor,
                            valueParameterDescriptor,
                            memberScope,
                            parameter, trace
                    );
                    classDescriptor.getBuilder().addPropertyDescriptor(propertyDescriptor);
                    c.getPrimaryConstructorParameterProperties().put(parameter, propertyDescriptor);
                }
                else {
                    notProperties.add(valueParameterDescriptor);
                }
            }

            if (classDescriptor.getKind() != ClassKind.TRAIT) {
                classDescriptor.setPrimaryConstructor(constructorDescriptor);
                classDescriptor.addConstructorParametersToInitializersScope(notProperties);
            }
        }
    }

    private void checkRedeclarationsInPackages(@NotNull TopDownAnalysisContext c) {
        for (MutablePackageFragmentDescriptor packageFragment : Sets.newHashSet(c.getPackageFragments().values())) {
            PackageViewDescriptor packageView = packageFragment.getContainingDeclaration().getPackage(packageFragment.getFqName());
            JetScope packageViewScope = packageView.getMemberScope();
            Multimap<Name, DeclarationDescriptor> simpleNameDescriptors = packageFragment.getMemberScope().getDeclaredDescriptorsAccessibleBySimpleName();
            for (Name name : simpleNameDescriptors.keySet()) {
                // Keep only properties with no receiver
                Collection<DeclarationDescriptor> descriptors = Collections2.filter(simpleNameDescriptors.get(name), new Predicate<DeclarationDescriptor>() {
                    @Override
                    public boolean apply(@Nullable DeclarationDescriptor descriptor) {
                        if (descriptor instanceof PropertyDescriptor) {
                            PropertyDescriptor propertyDescriptor = (PropertyDescriptor)descriptor;
                            return propertyDescriptor.getReceiverParameter() == null;
                        }
                        return true;
                    }
                });
                ContainerUtil.addIfNotNull(descriptors, packageViewScope.getPackage(name));

                if (descriptors.size() > 1) {
                    for (DeclarationDescriptor declarationDescriptor : descriptors) {
                        for (PsiElement declaration : getDeclarationsByDescriptor(declarationDescriptor)) {
                            assert declaration != null : "Null declaration for descriptor: " + declarationDescriptor + " " +
                                                         (declarationDescriptor != null ? DescriptorRenderer.FQ_NAMES_IN_TYPES.render(declarationDescriptor) : "");
                            trace.report(REDECLARATION.on(declaration, declarationDescriptor.getName().asString()));
                        }
                    }
                }
            }
        }
    }

    @NotNull
    private Collection<PsiElement> getDeclarationsByDescriptor(@NotNull DeclarationDescriptor declarationDescriptor) {
        Collection<PsiElement> declarations;
        if (declarationDescriptor instanceof PackageViewDescriptor) {
            final PackageViewDescriptor aPackage = (PackageViewDescriptor)declarationDescriptor;
            Collection<JetFile> files = trace.get(BindingContext.PACKAGE_TO_FILES, aPackage.getFqName());

            if (files == null) {
                return Collections.emptyList(); // package can be defined out of Kotlin sources, e. g. in library or Java code
            }

            declarations = Collections2.transform(files, new Function<JetFile, PsiElement>() {
                @Override
                public PsiElement apply(@Nullable JetFile file) {
                    assert file != null : "File is null for aPackage " + aPackage;
                    return file.getPackageDirective().getNameIdentifier();
                }
            });
        }
        else {
            declarations = Collections.singletonList(BindingContextUtils.descriptorToDeclaration(trace.getBindingContext(), declarationDescriptor));
        }
        return declarations;
    }

    public void checkRedeclarationsInInnerClassNames(@NotNull TopDownAnalysisContext c) {
        for (ClassDescriptorWithResolutionScopes classDescriptor : c.getDeclaredClasses().values()) {
            if (classDescriptor.getKind() == ClassKind.CLASS_OBJECT) {
                // Class objects should be considered during analysing redeclarations in classes
                continue;
            }

            Collection<DeclarationDescriptor> allDescriptors = classDescriptor.getScopeForMemberLookup().getOwnDeclaredDescriptors();

            ClassDescriptorWithResolutionScopes classObj = classDescriptor.getClassObjectDescriptor();
            if (classObj != null) {
                Collection<DeclarationDescriptor> classObjDescriptors = classObj.getScopeForMemberLookup().getOwnDeclaredDescriptors();
                if (!classObjDescriptors.isEmpty()) {
                    allDescriptors = Lists.newArrayList(allDescriptors);
                    allDescriptors.addAll(classObjDescriptors);
                }
            }

            Multimap<Name, DeclarationDescriptor> descriptorMap = HashMultimap.create();
            for (DeclarationDescriptor desc : allDescriptors) {
                if (desc instanceof ClassDescriptor || desc instanceof PropertyDescriptor) {
                    descriptorMap.put(desc.getName(), desc);
                }
            }

            reportRedeclarations(descriptorMap);
        }
    }

    private void reportRedeclarations(@NotNull Multimap<Name, DeclarationDescriptor> descriptorMap) {
        Set<Pair<PsiElement, Name>> redeclarations = Sets.newHashSet();
        for (Name name : descriptorMap.keySet()) {
            Collection<DeclarationDescriptor> descriptors = descriptorMap.get(name);
            if (descriptors.size() > 1) {
                // We mustn't compare PropertyDescriptor with PropertyDescriptor because we do this at OverloadResolver
                for (DeclarationDescriptor descriptor : descriptors) {
                    if (descriptor instanceof ClassDescriptor) {
                        for (DeclarationDescriptor descriptor2 : descriptors) {
                            if (descriptor == descriptor2) {
                                continue;
                            }

                            redeclarations.add(Pair.create(
                                    BindingContextUtils.classDescriptorToDeclaration(trace.getBindingContext(), (ClassDescriptor) descriptor),
                                    descriptor.getName()));
                            if (descriptor2 instanceof PropertyDescriptor) {
                                redeclarations.add(Pair.create(
                                        BindingContextUtils.descriptorToDeclaration(trace.getBindingContext(), descriptor2),
                                        descriptor2.getName()));
                            }
                        }
                    }
                }
            }
        }
        for (Pair<PsiElement, Name> redeclaration : redeclarations) {
            trace.report(REDECLARATION.on(redeclaration.getFirst(), redeclaration.getSecond().asString()));
        }
    }

    public void checkRedeclarationsInPackages(@NotNull KotlinCodeAnalyzer resolveSession, @NotNull Multimap<FqName, JetElement> topLevelFqNames) {
        for (Map.Entry<FqName, Collection<JetElement>> entry : topLevelFqNames.asMap().entrySet()) {
            FqName fqName = entry.getKey();
            Collection<JetElement> declarationsOrPackageDirectives = entry.getValue();

            if (fqName.isRoot()) continue;

            Set<DeclarationDescriptor> descriptors = getTopLevelDescriptorsByFqName(resolveSession, fqName);

            if (descriptors.size() > 1) {
                for (JetElement declarationOrPackageDirective : declarationsOrPackageDirectives) {
                    PsiElement reportAt = declarationOrPackageDirective instanceof JetNamedDeclaration
                                          ? declarationOrPackageDirective
                                          : ((JetPackageDirective) declarationOrPackageDirective).getNameIdentifier();
                    trace.report(Errors.REDECLARATION.on(reportAt, fqName.shortName().asString()));
                }
            }
        }
    }

    @NotNull
    private static Set<DeclarationDescriptor> getTopLevelDescriptorsByFqName(@NotNull KotlinCodeAnalyzer resolveSession, @NotNull FqName fqName) {
        FqName parentFqName = fqName.parent();

        Set<DeclarationDescriptor> descriptors = new HashSet<DeclarationDescriptor>();

        LazyPackageDescriptor parentFragment = resolveSession.getPackageFragment(parentFqName);
        if (parentFragment != null) {
            // Filter out extension properties
            descriptors.addAll(
                    KotlinPackage.filter(
                            parentFragment.getMemberScope().getProperties(fqName.shortName()),
                            new Function1<VariableDescriptor, Boolean>() {
                                @Override
                                public Boolean invoke(VariableDescriptor descriptor) {
                                    return descriptor.getReceiverParameter() == null;
                                }
                            }
                    )
            );
        }

        ContainerUtil.addIfNotNull(descriptors, resolveSession.getPackageFragment(fqName));

        descriptors.addAll(resolveSession.getTopLevelClassDescriptors(fqName));
        return descriptors;
    }


}
