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

package org.jetbrains.jet.plugin.libraries;

import com.google.common.base.Predicate;
import com.google.common.collect.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.GlobalSearchScopes;
import com.intellij.psi.stubs.StringStubIndexExtension;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
import org.jetbrains.jet.lang.DefaultModuleConfiguration;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.lazy.FileBasedDeclarationProviderFactory;
import org.jetbrains.jet.lang.resolve.lazy.ResolveSession;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.jet.plugin.stubindex.JetFullClassNameIndex;
import org.jetbrains.jet.renderer.DescriptorRenderer;
import org.jetbrains.jet.util.CommonSuppliers;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class JetSourceNavigationHelper {
    private static boolean forceResolve = false;

    private JetSourceNavigationHelper() {
    }

    @Nullable
    public static JetClassOrObject getSourceClassOrObject(@NotNull JetClassOrObject decompiledClassOrObject) {
        if (decompiledClassOrObject instanceof JetObjectDeclaration && decompiledClassOrObject.getParent() instanceof JetClassObject) {
            // class object case

            JetClass decompiledClass = PsiTreeUtil.getParentOfType(decompiledClassOrObject, JetClass.class);
            assert decompiledClass != null;

            JetClass sourceClass = (JetClass) getSourceForNamedClassOrObject(decompiledClass);
            if (sourceClass == null) {
                return null;
            }

            if (sourceClass.hasModifier(JetTokens.ENUM_KEYWORD)) {
                return sourceClass;
            }

            JetClassObject classObject = sourceClass.getClassObject();
            assert classObject != null;
            return classObject.getObjectDeclaration();
        }
        return getSourceForNamedClassOrObject(decompiledClassOrObject);
    }

    @NotNull
    private static GlobalSearchScope createLibrarySourcesScope(@NotNull JetNamedDeclaration decompiledDeclaration) {
        JetFile containingFile = (JetFile) decompiledDeclaration.getContainingFile();
        VirtualFile libraryFile = containingFile.getVirtualFile();
        if (libraryFile == null) {
            return GlobalSearchScope.EMPTY_SCOPE;
        }

        Project project = decompiledDeclaration.getProject();
        ProjectFileIndex projectFileIndex = ProjectFileIndex.SERVICE.getInstance(project);

        GlobalSearchScope resultScope = GlobalSearchScope.EMPTY_SCOPE;
        for (OrderEntry orderEntry : projectFileIndex.getOrderEntriesForFile(libraryFile)) {
            for (VirtualFile sourceDir : orderEntry.getFiles(OrderRootType.SOURCES)) {
                resultScope = resultScope.uniteWith(GlobalSearchScopes.directoryScope(project, sourceDir, true));
            }
        }
        return resultScope;
    }

    private static List<JetFile> getContainingFiles(@NotNull Iterable<? extends JetNamedDeclaration> declarations) {
        Set<JetFile> result = Sets.newHashSet();
        for (JetNamedDeclaration declaration : declarations) {
            PsiFile containingFile = declaration.getContainingFile();
            if (containingFile instanceof JetFile) {
                result.add((JetFile) containingFile);
            }
        }
        return Lists.newArrayList(result);
    }

    private static boolean haveRenamesInImports(@NotNull List<JetFile> files) {
        for (JetFile file : files) {
            for (JetImportDirective importDirective : file.getImportDirectives()) {
                if (importDirective.getAliasName() != null) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String getTypeShortName(@NotNull JetTypeReference typeReference) {
        JetTypeElement typeElement = typeReference.getTypeElement();
        assert typeElement != null;
        return typeElement.accept(new JetVisitor<String, Void>() {
            @Override
            public String visitDeclaration(JetDeclaration declaration, Void data) {
                throw new IllegalStateException("This visitor shouldn't be invoked for " + declaration.getClass());
            }

            @Override
            public String visitUserType(JetUserType type, Void data) {
                JetSimpleNameExpression referenceExpression = type.getReferenceExpression();
                assert referenceExpression != null;
                return referenceExpression.getReferencedName();
            }

            @Override
            public String visitFunctionType(JetFunctionType type, Void data) {
                KotlinBuiltIns builtIns = KotlinBuiltIns.getInstance();
                int parameterCount = type.getParameters().size();

                if (type.getReceiverTypeRef() == null) {
                    return builtIns.getFunction(parameterCount).getName().getName();
                }
                else {
                    return builtIns.getExtensionFunction(parameterCount).getName().getName();
                }
            }

            @Override
            public String visitNullableType(JetNullableType nullableType, Void data) {
                return nullableType.getInnerType().accept(this, null);
            }
        }, null);
    }

    private static boolean typesHaveSameShortName(@NotNull JetTypeReference a, @NotNull JetTypeReference b) {
        return getTypeShortName(a).equals(getTypeShortName(b));
    }

    @Nullable
    private static JetNamedDeclaration findSpecialProperty(@NotNull Name memberName, @NotNull JetClass containingClass) {
        // property constructor parameters
        List<JetParameter> constructorParameters = containingClass.getPrimaryConstructorParameters();
        for (JetParameter constructorParameter : constructorParameters) {
            if (memberName.equals(constructorParameter.getNameAsName()) && constructorParameter.getValOrVarNode() != null) {
                return constructorParameter;
            }
        }

        // enum entries
        if (containingClass.hasModifier(JetTokens.ENUM_KEYWORD)) {
            for (JetEnumEntry enumEntry : ContainerUtil.findAll(containingClass.getDeclarations(), JetEnumEntry.class)) {
                if (memberName.equals(enumEntry.getNameAsName())) {
                    return enumEntry;
                }
            }
        }
        return null;
    }

    @Nullable
    private static <Decl extends JetNamedDeclaration, Descr extends CallableDescriptor> JetNamedDeclaration
            getSourcePropertyOrFunction(
            @NotNull final Decl decompiledDeclaration,
            @NotNull final MemberNavigationStrategy<Decl, Descr> navigationStrategy
    ) {
        String memberNameAsString = decompiledDeclaration.getName();
        assert memberNameAsString != null;
        Name memberName = Name.identifier(memberNameAsString);

        PsiElement decompiledContainer = decompiledDeclaration.getParent();

        Collection<Decl> candidates;
        if (decompiledContainer instanceof JetFile) {
            candidates = getInitialTopLevelCandidates(decompiledDeclaration, navigationStrategy);
        }
        else if (decompiledContainer instanceof JetClassBody) {
            JetClassOrObject decompiledClassOrObject = (JetClassOrObject) decompiledContainer.getParent();
            JetClassOrObject sourceClassOrObject = getSourceClassOrObject(decompiledClassOrObject);

            candidates = sourceClassOrObject == null
                         ? Collections.<Decl>emptyList()
                         : getInitialMemberCandidates(sourceClassOrObject, memberName,
                                                      navigationStrategy.getDeclarationClass());

            if (candidates.isEmpty()) {
                if (decompiledDeclaration instanceof JetProperty && sourceClassOrObject instanceof JetClass) {
                    return findSpecialProperty(memberName, (JetClass) sourceClassOrObject);
                }
            }
        }
        else {
            throw new IllegalStateException("Unexpected container of decompiled declaration: "
                                            + decompiledContainer.getClass().getSimpleName());
        }

        if (!forceResolve) {
            candidates = filterByReceiverPresenceAndParametersCount(decompiledDeclaration, navigationStrategy, candidates);

            if (candidates.size() <= 1) {
                return candidates.isEmpty() ? null : candidates.iterator().next();
            }

            if (!haveRenamesInImports(getContainingFiles(candidates))) {
                candidates = filterByReceiverAndParameterTypes(decompiledDeclaration, navigationStrategy, candidates);

                if (candidates.size() <= 1) {
                    return candidates.isEmpty() ? null : candidates.iterator().next();
                }
            }
        }

        Project project = decompiledDeclaration.getProject();
        FileBasedDeclarationProviderFactory providerFactory = new FileBasedDeclarationProviderFactory(getContainingFiles(candidates),
                new Predicate<FqName>() {
                    @Override
                    public boolean apply(@Nullable FqName fqName) {
                        return KotlinBuiltIns.BUILT_INS_PACKAGE_FQ_NAME.equals(fqName);
                    }
                });
        ResolveSession resolveSession = new ResolveSession(
                project,
                new ModuleDescriptor(Name.special("<library module>")),
                DefaultModuleConfiguration.createStandardConfiguration(project),
                providerFactory);

        JetTypeReference receiverType = navigationStrategy.getReceiverType(decompiledDeclaration);
        for (Decl candidate : candidates) {
            //noinspection unchecked
            Descr candidateDescriptor = (Descr) resolveSession.resolveToDescriptor(candidate);
            if (receiversMatch(receiverType, candidateDescriptor.getReceiverParameter())
                    && valueParametersTypesMatch(navigationStrategy, decompiledDeclaration, candidateDescriptor)
                    && typeParametersMatch((JetTypeParameterListOwner) decompiledDeclaration, candidateDescriptor.getTypeParameters())) {
                return candidate;
            }
        }

        return null;
    }

    @Nullable
    private static JetClassOrObject getSourceForNamedClassOrObject(@NotNull JetClassOrObject decompiledClassOrObject) {
        FqName classFqName = JetPsiUtil.getFQName((JetNamedDeclaration) decompiledClassOrObject);
        assert classFqName != null;

        GlobalSearchScope librarySourcesScope = createLibrarySourcesScope((JetNamedDeclaration) decompiledClassOrObject);
        if (librarySourcesScope == GlobalSearchScope.EMPTY_SCOPE) { // .getProject() == null for EMPTY_SCOPE, and this breaks code
            return null;
        }
        Collection<JetClassOrObject> classes = JetFullClassNameIndex.getInstance()
                .get(classFqName.getFqName(), decompiledClassOrObject.getProject(), librarySourcesScope);
        if (classes.isEmpty()) {
            return null;
        }
        return classes.iterator().next(); // if there are more than one class with this FQ, find first of them
    }

    @NotNull
    private static <Decl extends JetNamedDeclaration, Descr extends CallableDescriptor> Collection<Decl> getInitialTopLevelCandidates(
            @NotNull Decl decompiledDeclaration,
            @NotNull MemberNavigationStrategy<Decl, Descr> navigationStrategy
    ) {
        FqName memberFqName = JetPsiUtil.getFQName(decompiledDeclaration);
        assert memberFqName != null;

        GlobalSearchScope librarySourcesScope = createLibrarySourcesScope(decompiledDeclaration);
        if (librarySourcesScope == GlobalSearchScope.EMPTY_SCOPE) { // .getProject() == null for EMPTY_SCOPE, and this breaks code
            return Collections.emptyList();
        }
        StringStubIndexExtension<Decl> index = navigationStrategy.getIndexForTopLevelMembers();
        return index.get(memberFqName.getFqName(), decompiledDeclaration.getProject(), librarySourcesScope);
    }

    @NotNull
    private static <Decl extends JetNamedDeclaration> List<Decl> getInitialMemberCandidates(
            @NotNull JetClassOrObject sourceClassOrObject,
            @NotNull final Name name,
            @NotNull Class<Decl> declarationClass
    ) {
        List<Decl> allByClass = ContainerUtil.findAll(sourceClassOrObject.getDeclarations(), declarationClass);
        return ContainerUtil.filter(allByClass, new Condition<Decl>() {
            @Override
            public boolean value(Decl declaration) {
                return name.equals(declaration.getNameAsSafeName());
            }
        });
    }

    @NotNull
    private static <Decl extends JetNamedDeclaration, Descr extends CallableDescriptor> List<Decl> filterByReceiverPresenceAndParametersCount(
            final @NotNull Decl decompiledDeclaration,
            final @NotNull MemberNavigationStrategy<Decl, Descr> navigationStrategy,
            @NotNull Collection<Decl> candidates
    ) {
        final JetTypeReference decompiledReceiver = navigationStrategy.getReceiverType(decompiledDeclaration);
        final int decompiledParametersCount = navigationStrategy.getValueParameters(decompiledDeclaration).size();

        return ContainerUtil.filter(candidates, new Condition<Decl>() {
            @Override
            public boolean value(Decl candidate) {
                boolean sameReceiverPresence = (navigationStrategy.getReceiverType(candidate) != null) ==
                                               (decompiledReceiver != null);
                boolean sameParameterCount = navigationStrategy.getValueParameters(candidate).size() == decompiledParametersCount;
                return sameReceiverPresence && sameParameterCount;
            }
        });
    }

    @NotNull
    private static <Decl extends JetNamedDeclaration, Descr extends CallableDescriptor> List<Decl> filterByReceiverAndParameterTypes(
            final @NotNull Decl decompiledDeclaration,
            final @NotNull MemberNavigationStrategy<Decl, Descr> navigationStrategy,
            @NotNull Collection<Decl> candidates
    ) {
        final JetTypeReference decompiledReceiver = navigationStrategy.getReceiverType(decompiledDeclaration);

        return ContainerUtil.filter(candidates, new Condition<Decl>() {
            @Override
            public boolean value(Decl candidate) {
                if (decompiledReceiver != null) {
                    JetTypeReference candidateReceiver = navigationStrategy.getReceiverType(candidate);
                    assert candidateReceiver != null;
                    if (!typesHaveSameShortName(decompiledReceiver, candidateReceiver)) {
                        return false;
                    }
                }

                return parameterShortTypesMatch(navigationStrategy, candidate, decompiledDeclaration);
            }
        });
    }

    private static boolean receiversMatch(
            @Nullable JetTypeReference receiverTypeRef,
            @Nullable ReceiverParameterDescriptor receiverParameter
    ) {
        if (receiverTypeRef == null && receiverParameter == null) {
            return true;
        }
        if (receiverTypeRef != null && receiverParameter != null) {
            return receiverTypeRef.getText().equals(DescriptorRenderer.TEXT.renderType(receiverParameter.getType()));
        }
        return false;
    }

    private static <Decl extends JetNamedDeclaration, Descr extends CallableDescriptor> boolean valueParametersTypesMatch(
            @NotNull MemberNavigationStrategy<Decl, Descr> navigationStrategy,
            @NotNull Decl declaration,
            @NotNull Descr descriptor
    ) {
        List<JetParameter> declarationParameters = navigationStrategy.getValueParameters(declaration);
        List<ValueParameterDescriptor> descriptorParameters = navigationStrategy.getValueParameters(descriptor);
        if (descriptorParameters.size() != declarationParameters.size()) {
            return false;
        }

        for (int i = 0; i < descriptorParameters.size(); i++) {
            ValueParameterDescriptor descriptorParameter = descriptorParameters.get(i);
            JetParameter declarationParameter = declarationParameters.get(i);
            JetTypeReference typeReference = declarationParameter.getTypeReference();
            if (typeReference == null) {
                return false;
            }
            JetModifierList modifierList = declarationParameter.getModifierList();
            boolean varargInDeclaration = modifierList != null && modifierList.hasModifier(JetTokens.VARARG_KEYWORD);
            boolean varargInDescriptor = descriptorParameter.getVarargElementType() != null;
            if (varargInDeclaration != varargInDescriptor) {
                return false;
            }
            String declarationTypeText = typeReference.getText();

            JetType typeToRender = varargInDeclaration ? descriptorParameter.getVarargElementType() : descriptorParameter.getType();
            assert typeToRender != null;
            String descriptorParameterText = DescriptorRenderer.TEXT.renderType(typeToRender);
            if (!declarationTypeText.equals(descriptorParameterText)) {
                return false;
            }
        }
        return true;
    }

    private static <Decl extends JetNamedDeclaration> boolean parameterShortTypesMatch(
            @NotNull MemberNavigationStrategy<Decl, ?> navigationStrategy,
            @NotNull Decl a,
            @NotNull Decl b
    ) {
        List<JetParameter> aParameters = navigationStrategy.getValueParameters(a);
        List<JetParameter> bParameters = navigationStrategy.getValueParameters(b);
        if (aParameters.size() != bParameters.size()) {
            return false;
        }
        for (int i = 0; i < aParameters.size(); i++) {
            JetTypeReference aType = aParameters.get(i).getTypeReference();
            JetTypeReference bType = bParameters.get(i).getTypeReference();

            assert aType != null;
            assert bType != null;

            if (!typesHaveSameShortName(aType, bType)) {
                return false;
            }
        }
        return true;
    }

    private static boolean typeParametersMatch(
            @NotNull JetTypeParameterListOwner typeParameterListOwner,
            @NotNull List<TypeParameterDescriptor> typeParameterDescriptors
    ) {
        List<JetTypeParameter> decompiledParameters = typeParameterListOwner.getTypeParameters();
        if (decompiledParameters.size() != typeParameterDescriptors.size()) {
            return false;
        }

        Multimap<Name, String> decompiledParameterToBounds = Multimaps.newSetMultimap(
                Maps.<Name, Collection<String>>newHashMap(), CommonSuppliers.<String>getHashSetSupplier());
        for (JetTypeParameter parameter : decompiledParameters) {
            JetTypeReference extendsBound = parameter.getExtendsBound();
            if (extendsBound != null) {
                decompiledParameterToBounds.put(parameter.getNameAsName(), extendsBound.getText());
            }
        }

        for (JetTypeConstraint typeConstraint : typeParameterListOwner.getTypeConstraints()) {
            JetSimpleNameExpression typeParameterName = typeConstraint.getSubjectTypeParameterName();
            assert typeParameterName != null;

            JetTypeReference bound = typeConstraint.getBoundTypeReference();
            assert bound != null;

            decompiledParameterToBounds.put(typeParameterName.getReferencedNameAsName(), bound.getText());
        }

        for (int i = 0; i < decompiledParameters.size(); i++) {
            JetTypeParameter decompiledParameter = decompiledParameters.get(i);
            TypeParameterDescriptor descriptor = typeParameterDescriptors.get(i);

            Name name = decompiledParameter.getNameAsName();
            assert name != null;
            if (!name.equals(descriptor.getName())) {
                return false;
            }

            Set<String> descriptorUpperBounds = Sets.newHashSet(ContainerUtil.map(
                    descriptor.getUpperBounds(), new Function<JetType, String>() {
                                                    @Override
                                                    public String fun(JetType type) {
                                                        return DescriptorRenderer.TEXT.renderType(type);
                                                    }
                                                }));

            Set<String> decompiledUpperBounds = decompiledParameterToBounds.get(descriptor.getName()).isEmpty()
                    ? Sets.newHashSet(DescriptorRenderer.TEXT.renderType(KotlinBuiltIns.getInstance().getDefaultBound()))
                    : Sets.newHashSet(decompiledParameterToBounds.get(descriptor.getName()));
            if (!descriptorUpperBounds.equals(decompiledUpperBounds)) {
                return false;
            }
        }
        return true;
    }

    @Nullable
    public static JetNamedDeclaration getSourceProperty(final @NotNull JetProperty decompiledProperty) {
        return getSourcePropertyOrFunction(decompiledProperty, new MemberNavigationStrategy.PropertyStrategy());
    }

    @Nullable
    public static JetNamedDeclaration getSourceFunction(final @NotNull JetNamedFunction decompiledFunction) {
        return getSourcePropertyOrFunction(decompiledFunction, new MemberNavigationStrategy.FunctionStrategy());
    }

    @TestOnly
    static void setForceResolve(boolean forceResolve) {
        JetSourceNavigationHelper.forceResolve = forceResolve;
    }
}
