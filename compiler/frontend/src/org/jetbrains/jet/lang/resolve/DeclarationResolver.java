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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.impl.MutableClassDescriptor;
import org.jetbrains.jet.lang.descriptors.impl.MutableClassDescriptorLite;
import org.jetbrains.jet.lang.descriptors.impl.NamespaceDescriptorImpl;
import org.jetbrains.jet.lang.descriptors.impl.NamespaceLikeBuilder;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;
import org.jetbrains.jet.lang.types.ErrorUtils;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.renderer.DescriptorRenderer;

import javax.inject.Inject;
import java.util.*;

import static org.jetbrains.jet.lang.diagnostics.Errors.*;

public class DeclarationResolver {
    @NotNull
    private AnnotationResolver annotationResolver;
    @NotNull
    private TopDownAnalysisContext context;
    @NotNull
    private ImportsResolver importsResolver;
    @NotNull
    private DescriptorResolver descriptorResolver;
    @NotNull
    private ScriptHeaderResolver scriptHeaderResolver;
    @NotNull
    private BindingTrace trace;


    @Inject
    public void setAnnotationResolver(@NotNull AnnotationResolver annotationResolver) {
        this.annotationResolver = annotationResolver;
    }

    @Inject
    public void setContext(@NotNull TopDownAnalysisContext context) {
        this.context = context;
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

    @Inject
    public void setScriptHeaderResolver(@NotNull ScriptHeaderResolver scriptHeaderResolver) {
        this.scriptHeaderResolver = scriptHeaderResolver;
    }



    public void process(@NotNull JetScope rootScope) {
        resolveConstructorHeaders();
        resolveAnnotationStubsOnClassesAndConstructors();
        resolveFunctionAndPropertyHeaders();
        createFunctionsForDataClasses();
        importsResolver.processMembersImports(rootScope);
        checkRedeclarationsInNamespaces();
        checkRedeclarationsInInnerClassNames();
    }



    private void resolveConstructorHeaders() {
        for (Map.Entry<JetClass, MutableClassDescriptor> entry : context.getClasses().entrySet()) {
            JetClass jetClass = entry.getKey();
            MutableClassDescriptor classDescriptor = entry.getValue();

            processPrimaryConstructor(classDescriptor, jetClass);
        }
    }

    private void resolveAnnotationStubsOnClassesAndConstructors() {
        for (Map.Entry<JetClass, MutableClassDescriptor> entry : context.getClasses().entrySet()) {
            JetClass jetClass = entry.getKey();
            MutableClassDescriptor descriptor = entry.getValue();
            resolveAnnotationsForClassOrObject(annotationResolver, jetClass, descriptor);
        }
        for (Map.Entry<JetObjectDeclaration, MutableClassDescriptor> entry : context.getObjects().entrySet()) {
            JetObjectDeclaration objectDeclaration = entry.getKey();
            MutableClassDescriptor descriptor = entry.getValue();
            resolveAnnotationsForClassOrObject(annotationResolver, objectDeclaration, descriptor);
        }
    }

    private void resolveAnnotationsForClassOrObject(AnnotationResolver annotationResolver, JetClassOrObject jetClass, MutableClassDescriptor descriptor) {
        JetModifierList modifierList = jetClass.getModifierList();
        if (modifierList != null) {
            descriptor.getAnnotations().addAll(annotationResolver.resolveAnnotations(descriptor.getScopeForSupertypeResolution(), modifierList.getAnnotationEntries(), trace));
        }
    }

    private void resolveFunctionAndPropertyHeaders() {
        for (Map.Entry<JetFile, WritableScope> entry : context.getNamespaceScopes().entrySet()) {
            JetFile namespace = entry.getKey();
            WritableScope namespaceScope = entry.getValue();
            NamespaceLikeBuilder namespaceDescriptor = context.getNamespaceDescriptors().get(namespace).getBuilder();

            resolveFunctionAndPropertyHeaders(namespace.getDeclarations(), namespaceScope, namespaceScope, namespaceScope, namespaceDescriptor);
        }
        for (Map.Entry<JetClass, MutableClassDescriptor> entry : context.getClasses().entrySet()) {
            JetClass jetClass = entry.getKey();
            MutableClassDescriptor classDescriptor = entry.getValue();

            JetClassBody jetClassBody = jetClass.getBody();
            if (classDescriptor.getKind() == ClassKind.ANNOTATION_CLASS && jetClassBody != null) {
                trace.report(ANNOTATION_CLASS_WITH_BODY.on(jetClassBody));
            }

            resolveFunctionAndPropertyHeaders(
                    jetClass.getDeclarations(), classDescriptor.getScopeForMemberResolution(),
                    classDescriptor.getScopeForInitializers(), classDescriptor.getScopeForMemberResolution(),
                    classDescriptor.getBuilder());
            //            processPrimaryConstructor(classDescriptor, jetClass);
            //            for (JetSecondaryConstructor jetConstructor : jetClass.getSecondaryConstructors()) {
            //                processSecondaryConstructor(classDescriptor, jetConstructor);
            //            }
        }
        for (Map.Entry<JetObjectDeclaration, MutableClassDescriptor> entry : context.getObjects().entrySet()) {
            JetObjectDeclaration object = entry.getKey();
            MutableClassDescriptor classDescriptor = entry.getValue();

            resolveFunctionAndPropertyHeaders(object.getDeclarations(), classDescriptor.getScopeForMemberResolution(),
                    classDescriptor.getScopeForInitializers(), classDescriptor.getScopeForMemberResolution(),
                    classDescriptor.getBuilder());
        }

        scriptHeaderResolver.resolveScriptDeclarations();

        // TODO : Extensions
    }

    private void resolveFunctionAndPropertyHeaders(@NotNull List<JetDeclaration> declarations,
            final @NotNull JetScope scopeForFunctions,
            final @NotNull JetScope scopeForPropertyInitializers, final @NotNull JetScope scopeForPropertyAccessors,
            final @NotNull NamespaceLikeBuilder namespaceLike)
    {
        for (JetDeclaration declaration : declarations) {
            declaration.accept(new JetVisitorVoid() {
                @Override
                public void visitNamedFunction(JetNamedFunction function) {
                    SimpleFunctionDescriptor functionDescriptor = descriptorResolver.resolveFunctionDescriptor(namespaceLike.getOwnerForChildren(), scopeForFunctions, function, trace);
                    namespaceLike.addFunctionDescriptor(functionDescriptor);
                    context.getFunctions().put(function, functionDescriptor);
                    context.registerDeclaringScope(function, scopeForFunctions);
                }

                @Override
                public void visitProperty(JetProperty property) {
                    PropertyDescriptor propertyDescriptor = descriptorResolver.resolvePropertyDescriptor(namespaceLike.getOwnerForChildren(), scopeForPropertyInitializers, property, trace);
                    namespaceLike.addPropertyDescriptor(propertyDescriptor);
                    context.getProperties().put(property, propertyDescriptor);
                    context.registerDeclaringScope(property, scopeForPropertyInitializers);
                    JetPropertyAccessor getter = property.getGetter();
                    if (getter != null) {
                        context.registerDeclaringScope(getter, scopeForPropertyAccessors);
                    }
                    JetPropertyAccessor setter = property.getSetter();
                    if (setter != null) {
                        context.registerDeclaringScope(setter, scopeForPropertyAccessors);
                    }
                }

                @Override
                public void visitObjectDeclaration(JetObjectDeclaration declaration) {
                    PropertyDescriptor propertyDescriptor = descriptorResolver.resolveObjectDeclarationAsPropertyDescriptor(
                            namespaceLike.getOwnerForChildren(), declaration, context.getObjects().get(declaration), trace);

                    namespaceLike.addPropertyDescriptor(propertyDescriptor);
                }

                @Override
                public void visitEnumEntry(JetEnumEntry enumEntry) {
                    // FIX: Bad cast
                    MutableClassDescriptorLite classObjectDescriptor =
                            ((MutableClassDescriptorLite)namespaceLike.getOwnerForChildren()).getClassObjectDescriptor();
                    assert classObjectDescriptor != null;
                    PropertyDescriptor propertyDescriptor = descriptorResolver.resolveObjectDeclarationAsPropertyDescriptor(
                            classObjectDescriptor, enumEntry, context.getClasses().get(enumEntry), trace);
                    classObjectDescriptor.getBuilder().addPropertyDescriptor(propertyDescriptor);
                }
            });
        }
    }

    private void createFunctionsForDataClasses() {
        for (Map.Entry<JetClass, MutableClassDescriptor> entry : context.getClasses().entrySet()) {
            JetClass jetClass = entry.getKey();
            MutableClassDescriptor classDescriptor = entry.getValue();

            if (jetClass.hasPrimaryConstructor() && KotlinBuiltIns.getInstance().isData(classDescriptor)) {
                ConstructorDescriptor constructor = DescriptorUtils.getConstructorOfDataClass(classDescriptor);
                createComponentFunctions(classDescriptor, constructor);
                createCopyFunction(classDescriptor, constructor);
            }
        }
    }

    private void createComponentFunctions(@NotNull MutableClassDescriptor classDescriptor, @NotNull ConstructorDescriptor constructorDescriptor) {
        int parameterIndex = 0;
        for (ValueParameterDescriptor parameter : constructorDescriptor.getValueParameters()) {
            if (!ErrorUtils.isErrorType(parameter.getType())) {
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

    private void processPrimaryConstructor(MutableClassDescriptor classDescriptor, JetClass klass) {
        if (classDescriptor.getKind() == ClassKind.TRAIT) {
            JetParameterList primaryConstructorParameterList = klass.getPrimaryConstructorParameterList();
            if (primaryConstructorParameterList != null) {
                trace.report(CONSTRUCTOR_IN_TRAIT.on(primaryConstructorParameterList));
            }
        }

        // TODO : not all the parameters are real properties
        JetScope memberScope = classDescriptor.getScopeForSupertypeResolution();
        ConstructorDescriptor constructorDescriptor = descriptorResolver.resolvePrimaryConstructorDescriptor(memberScope, classDescriptor, klass, trace);
        if (constructorDescriptor != null) {
            List<ValueParameterDescriptor> valueParameterDescriptors = constructorDescriptor.getValueParameters();
            List<JetParameter> primaryConstructorParameters = klass.getPrimaryConstructorParameters();
            assert valueParameterDescriptors.size() == primaryConstructorParameters.size();
            for (ValueParameterDescriptor valueParameterDescriptor : valueParameterDescriptors) {
                JetParameter parameter = primaryConstructorParameters.get(valueParameterDescriptor.getIndex());
                if (parameter.getValOrVarNode() != null) {
                    PropertyDescriptor propertyDescriptor = descriptorResolver.resolvePrimaryConstructorParameterToAProperty(
                            classDescriptor,
                            valueParameterDescriptor,
                            memberScope,
                            parameter, trace
                    );
                    classDescriptor.getBuilder().addPropertyDescriptor(propertyDescriptor);
                    context.getPrimaryConstructorParameterProperties().put(parameter, propertyDescriptor);
                }
            }
            if (classDescriptor.getKind() != ClassKind.TRAIT) {
                classDescriptor.setPrimaryConstructor(constructorDescriptor, trace);
            }
        }
    }

    private void checkRedeclarationsInNamespaces() {
        for (NamespaceDescriptorImpl descriptor : context.getNamespaceDescriptors().values()) {
            Multimap<Name, DeclarationDescriptor> simpleNameDescriptors = descriptor.getMemberScope().getDeclaredDescriptorsAccessibleBySimpleName();
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
                if (descriptors.size() > 1) {
                    for (DeclarationDescriptor declarationDescriptor : descriptors) {
                        for (PsiElement declaration : getDeclarationsByDescriptor(declarationDescriptor)) {
                            assert declaration != null : "Null declaration for descriptor: " + declarationDescriptor + " " +
                                                         (declarationDescriptor != null ? DescriptorRenderer.TEXT.render(declarationDescriptor) : "");
                            trace.report(REDECLARATION.on(declaration, declarationDescriptor.getName().getName()));
                        }
                    }
                }
            }
        }
    }

    private Collection<PsiElement> getDeclarationsByDescriptor(DeclarationDescriptor declarationDescriptor) {
        Collection<PsiElement> declarations;
        if (declarationDescriptor instanceof NamespaceDescriptor) {
            final NamespaceDescriptor namespace = (NamespaceDescriptor)declarationDescriptor;
            Collection<JetFile> files = trace.get(BindingContext.NAMESPACE_TO_FILES, namespace);

            if (files == null) {
                throw new IllegalStateException("declarations corresponding to " + namespace + " are not found");
            }

            declarations = Collections2.transform(files, new Function<JetFile, PsiElement>() {
                @Override
                public PsiElement apply(@Nullable JetFile file) {
                    assert file != null : "File is null for namespace " + namespace;
                    return file.getNamespaceHeader().getNameIdentifier();
                }
            });
        }
        else {
            declarations = Collections.singletonList(BindingContextUtils.descriptorToDeclaration(trace.getBindingContext(), declarationDescriptor));
        }
        return declarations;
    }

    private void checkRedeclarationsInInnerClassNames() {
        for (MutableClassDescriptor classDescriptor : context.getClasses().values()) {
            Collection<DeclarationDescriptor> allDescriptors = classDescriptor.getScopeForMemberLookup().getOwnDeclaredDescriptors();

            MutableClassDescriptorLite classObj = classDescriptor.getClassObjectDescriptor();
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
            trace.report(REDECLARATION.on(redeclaration.getFirst(), redeclaration.getSecond().getName()));
        }
    }


}
