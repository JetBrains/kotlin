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

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.AnnotationResolver;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.DescriptorResolver;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.lazy.data.FilteringClassLikeInfo;
import org.jetbrains.jet.lang.resolve.lazy.data.JetClassInfoUtil;
import org.jetbrains.jet.lang.resolve.lazy.data.JetClassLikeInfo;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.*;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ClassReceiver;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.jetbrains.jet.lang.types.ErrorUtils;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeConstructor;
import org.jetbrains.jet.lang.types.TypeUtils;

import java.util.*;

/**
 * @author abreslav
 */
public class LazyClassDescriptor extends ClassDescriptorBase implements ClassDescriptorFromSource {

    private static final Predicate<Object> ONLY_ENUM_ENTIRES = Predicates.instanceOf(JetEnumEntry.class);
    private static final Predicate<JetType> VALID_SUPERTYPE = new Predicate<JetType>() {
        @Override
        public boolean apply(JetType type) {
            return !ErrorUtils.isErrorType(type) && (TypeUtils.getClassDescriptor(type) != null);
        }
    };
    private final ResolveSession resolveSession;
    private final JetClassLikeInfo originalClassInfo;
    private final ClassMemberDeclarationProvider declarationProvider;

    private final Name name;
    private final DeclarationDescriptor containingDeclaration;
    private final TypeConstructor typeConstructor;
    private final Modality modality;
    private final Visibility visibility;
    private final ClassKind kind;

    private ClassReceiver implicitReceiver;
    private List<AnnotationDescriptor> annotations;
    private ClassDescriptor classObjectDescriptor;
    private boolean classObjectDescriptorResolved = false;

    private final LazyClassMemberScope unsubstitutedMemberScope;
    private final JetScope unsubstitutedInnerClassesScope;

    private JetScope scopeForClassHeaderResolution;
    private JetScope scopeForMemberDeclarationResolution;
    private JetScope scopeForPropertyInitializerResolution;


    public LazyClassDescriptor(
            @NotNull ResolveSession resolveSession,
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull Name name,
            @NotNull JetClassLikeInfo classLikeInfo
    ) {
        this.resolveSession = resolveSession;

        if (classLikeInfo.getCorrespondingClassOrObject() != null) {
            this.resolveSession.getTrace().record(BindingContext.CLASS, classLikeInfo.getCorrespondingClassOrObject(), this);
        }

        this.originalClassInfo = classLikeInfo;
        JetClassLikeInfo classLikeInfoForMembers =
                classLikeInfo.getClassKind() != ClassKind.ENUM_CLASS ? classLikeInfo : noEnumEntries(classLikeInfo);
        this.declarationProvider = resolveSession.getDeclarationProviderFactory().getClassMemberDeclarationProvider(classLikeInfoForMembers);
        this.name = name;
        this.containingDeclaration = containingDeclaration;
        this.unsubstitutedMemberScope = new LazyClassMemberScope(resolveSession, declarationProvider, this);
        this.unsubstitutedInnerClassesScope = new InnerClassesScopeWrapper(unsubstitutedMemberScope);

        this.typeConstructor = new LazyClassTypeConstructor();

        this.kind = classLikeInfo.getClassKind();
        if (kind == ClassKind.OBJECT) {
            this.modality = Modality.FINAL;
        }
        else {
            Modality defaultModality = kind == ClassKind.TRAIT ? Modality.ABSTRACT : Modality.FINAL;
            JetModifierList modifierList = classLikeInfo.getModifierList();
            this.modality = DescriptorResolver.resolveModalityFromModifiers(modifierList, defaultModality);
        }
        JetModifierList modifierList = classLikeInfo.getModifierList();
        this.visibility = DescriptorResolver.resolveVisibilityFromModifiers(modifierList, Visibilities.INTERNAL);
    }

    @Override
    protected JetScope getScopeForMemberLookup() {
        return unsubstitutedMemberScope;
    }

    @NotNull
    @Override
    public JetScope getUnsubstitutedInnerClassesScope() {
        return unsubstitutedInnerClassesScope;
    }

    @NotNull
    public JetScope getScopeForClassHeaderResolution() {
        if (scopeForClassHeaderResolution == null) {
            WritableScopeImpl scope = new WritableScopeImpl(
                    resolveSession.getResolutionScope(declarationProvider.getOwnerInfo().getScopeAnchor()), this, RedeclarationHandler.DO_NOTHING, "Class Header Resolution");
            for (TypeParameterDescriptor typeParameterDescriptor : getTypeConstructor().getParameters()) {
                scope.addClassifierDescriptor(typeParameterDescriptor);
            }
            scope.changeLockLevel(WritableScope.LockLevel.READING);
            scopeForClassHeaderResolution = scope;
        }
        return scopeForClassHeaderResolution;
    }

    public JetScope getScopeForMemberDeclarationResolution() {
        if (scopeForMemberDeclarationResolution == null) {
            WritableScopeImpl scope = new WritableScopeImpl(
                    getScopeForMemberLookup(), this, RedeclarationHandler.DO_NOTHING, "Member Declaration Resolution");
            scope.addLabeledDeclaration(this);
            scope.importScope(getScopeForClassHeaderResolution());

            scope.changeLockLevel(WritableScope.LockLevel.READING);
            scopeForMemberDeclarationResolution = scope;
        }
        return scopeForMemberDeclarationResolution;
    }

    public JetScope getScopeForPropertyInitializerResolution() {
        ConstructorDescriptor primaryConstructor = getUnsubstitutedPrimaryConstructor();
        if (primaryConstructor == null) return getScopeForMemberDeclarationResolution();

        if (scopeForPropertyInitializerResolution == null) {
            WritableScopeImpl scope = new WritableScopeImpl(
                    getScopeForMemberDeclarationResolution(), this, RedeclarationHandler.DO_NOTHING, "Property Initializer Resolution");

            List<ValueParameterDescriptor> parameters = primaryConstructor.getValueParameters();
            for (ValueParameterDescriptor valueParameterDescriptor : parameters) {
                scope.addVariableDescriptor(valueParameterDescriptor);
            }

            scope.changeLockLevel(WritableScope.LockLevel.READING);
            scopeForPropertyInitializerResolution = scope;
        }
        return scopeForPropertyInitializerResolution;
    }

    @NotNull
    @Override
    public Collection<ConstructorDescriptor> getConstructors() {
        return unsubstitutedMemberScope.getConstructors();
    }

    @Override
    public ConstructorDescriptor getUnsubstitutedPrimaryConstructor() {
        return unsubstitutedMemberScope.getPrimaryConstructor();
    }

    @NotNull
    @Override
    public DeclarationDescriptor getOriginal() {
        return this;
    }

    @NotNull
    @Override
    public DeclarationDescriptor getContainingDeclaration() {
        return containingDeclaration;
    }

    @NotNull
    @Override
    public TypeConstructor getTypeConstructor() {
        return typeConstructor;
    }

    @Override
    public JetType getClassObjectType() {
        ClassDescriptor classObjectDescriptor = getClassObjectDescriptor();
        return classObjectDescriptor == null ? null : classObjectDescriptor.getDefaultType();
    }

    @Override
    public boolean isClassObjectAValue() {
        return true;
    }

    @Override
    public ClassDescriptor getClassObjectDescriptor() {
        if (!classObjectDescriptorResolved) {
            JetClassObject classObject = declarationProvider.getOwnerInfo().getClassObject();

            JetClassLikeInfo classObjectInfo = getClassObjectInfo(classObject);
            if (classObjectInfo != null) {
                Name classObjectName = getKind() == ClassKind.ENUM_CLASS
                                       ? Name.special("<class-object-for-" + getName() + ">")
                                       : JetPsiUtil.NO_NAME_PROVIDED;
                classObjectDescriptor = new LazyClassDescriptor(resolveSession, this, classObjectName, classObjectInfo);
            }
            classObjectDescriptorResolved = true;
        }
        return classObjectDescriptor;
    }

    @Nullable
    private JetClassLikeInfo getClassObjectInfo(JetClassObject classObject) {
        if (classObject != null) {
            if (!DescriptorUtils.inStaticContext(this)) {
                return null;
            }
            JetObjectDeclaration objectDeclaration = classObject.getObjectDeclaration();
            if (objectDeclaration != null) {
                return JetClassInfoUtil.createClassLikeInfo(objectDeclaration);
            }
        }
        else {
            if (getKind() == ClassKind.ENUM_CLASS) {
                // Enum classes always have class objects, and enum constants are their members
                return onlyEnumEntries(originalClassInfo);
            }
        }
        return null;
    }

    @NotNull
    @Override
    public ClassKind getKind() {
        return kind;
    }

    @NotNull
    @Override
    public Modality getModality() {
        return modality;
    }

    @NotNull
    @Override
    public Visibility getVisibility() {
        return visibility;
    }

    @NotNull
    @Override
    public ReceiverDescriptor getImplicitReceiver() {
        if (implicitReceiver == null) {
            implicitReceiver = new ClassReceiver(this);
        }
        return implicitReceiver;
    }

    @Override
    public List<AnnotationDescriptor> getAnnotations() {
        if (annotations == null) {
            JetClassLikeInfo classInfo = declarationProvider.getOwnerInfo();
            JetModifierList modifierList = classInfo.getModifierList();
            if (modifierList != null) {
                AnnotationResolver annotationResolver = resolveSession.getInjector().getAnnotationResolver();
                annotations = annotationResolver
                        .resolveAnnotations(resolveSession.getResolutionScope(classInfo.getScopeAnchor()), modifierList, resolveSession.getTrace());
            }
            else {
                annotations = Collections.emptyList();
            }
        }
        return annotations;
    }

    @NotNull
    @Override
    public Name getName() {
        return name;
    }

    @Override
    public String toString() {
        return "lazy class " + getName().toString();
    }

    private class LazyClassTypeConstructor implements TypeConstructor {
        private Collection<JetType> supertypes = null;
        private List<TypeParameterDescriptor> parameters = null;

        @NotNull
        @Override
        public List<TypeParameterDescriptor> getParameters() {
            if (parameters == null) {
                JetClassLikeInfo classInfo = declarationProvider.getOwnerInfo();
                List<JetTypeParameter> typeParameters = classInfo.getTypeParameters();
                parameters = new ArrayList<TypeParameterDescriptor>(typeParameters.size());

                for (int i = 0; i < typeParameters.size(); i++) {
                    parameters.add(new LazyTypeParameterDescriptor(resolveSession, LazyClassDescriptor.this, typeParameters.get(i), i));
                }
            }
            return parameters;
        }

        @NotNull
        @Override
        public Collection<? extends JetType> getSupertypes() {
            if (supertypes == null) {
                if (resolveSession.isClassSpecial(DescriptorUtils.getFQName(LazyClassDescriptor.this))) {
                    this.supertypes = Collections.emptyList();
                }
                else {
                    JetClassOrObject classOrObject = declarationProvider.getOwnerInfo().getCorrespondingClassOrObject();
                    if (classOrObject == null) {
                        this.supertypes = Collections.emptyList();
                    }
                    else {
                        List<JetType> allSupertypes = resolveSession.getInjector().getDescriptorResolver()
                                .resolveSupertypes(getScopeForClassHeaderResolution(),
                                                   classOrObject,
                                                   resolveSession.getTrace());
                        List<JetType> validSupertypes = Lists.newArrayList(Collections2.filter(allSupertypes, VALID_SUPERTYPE));
                        this.supertypes = validSupertypes;
                        findAndDisconnectLoopsInTypeHierarchy(validSupertypes);
                    }
                }
            }
            return supertypes;
        }

        private void findAndDisconnectLoopsInTypeHierarchy(List<JetType> supertypes) {
            for (Iterator<JetType> iterator = supertypes.iterator(); iterator.hasNext(); ) {
                JetType supertype = iterator.next();
                if (isReachable(supertype.getConstructor(), this, new HashSet<TypeConstructor>())) {
                    iterator.remove();
                }
            }
        }

        private boolean isReachable(TypeConstructor from, TypeConstructor to, Set<TypeConstructor> visited) {
            if (!visited.add(from)) return false;
            for (JetType supertype : from.getSupertypes()) {
                TypeConstructor supertypeConstructor = supertype.getConstructor();
                if (supertypeConstructor == to) {
                    return true;
                }
                if (isReachable(supertypeConstructor, to, visited)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean isSealed() {
            return !getModality().isOverridable();
        }

        @Override
        public ClassifierDescriptor getDeclarationDescriptor() {
            return LazyClassDescriptor.this;
        }

        @Override
        public List<AnnotationDescriptor> getAnnotations() {
            return Collections.emptyList(); // TODO
        }

        @Override
        public String toString() {
            return LazyClassDescriptor.this.getName().toString();
        }
    }

    private static JetClassLikeInfo noEnumEntries(JetClassLikeInfo classLikeInfo) {
        return new FilteringClassLikeInfo(classLikeInfo, Predicates.not(ONLY_ENUM_ENTIRES));
    }

    private static JetClassLikeInfo onlyEnumEntries(JetClassLikeInfo classLikeInfo) {
        return new FilteringClassLikeInfo(classLikeInfo, ONLY_ENUM_ENTIRES) {
            @Override
            public JetClassOrObject getCorrespondingClassOrObject() {
                return null;
            }

            @NotNull
            @Override
            public ClassKind getClassKind() {
                return ClassKind.OBJECT;
            }

            @NotNull
            @Override
            public List<? extends JetParameter> getPrimaryConstructorParameters() {
                return Collections.emptyList();
            }

            @NotNull
            @Override
            public List<JetTypeParameter> getTypeParameters() {
                return Collections.emptyList();
            }
        };
    }

}
