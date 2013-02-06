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

package org.jetbrains.jet.lang.resolve.lazy;

import com.google.common.collect.Sets;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.ClassifierDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptorVisitor;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.LazyScopeAdapter;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.lang.types.checker.JetTypeChecker;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.jet.util.lazy.RecursionIntolerantLazyValue;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class LazyTypeParameterDescriptor implements TypeParameterDescriptor, LazyDescriptor {
    private final ResolveSession resolveSession;

    private final JetTypeParameter jetTypeParameter;
    private final Variance variance;
    private final boolean reified;
    private final int index;
    private final LazyClassDescriptor containingDeclaration;
    private final Name name;

    private final LazyValue<TypeConstructor> typeConstructor;
    private final LazyValue<JetType> defaultType;
    private final LazyValue<Set<JetType>> upperBounds;
    private final LazyValue<JetType> upperBoundsAsType;

    public LazyTypeParameterDescriptor(
            @NotNull ResolveSession resolveSession,
            @NotNull LazyClassDescriptor containingDeclaration,
            @NotNull JetTypeParameter jetTypeParameter,
            int index) {
        this.resolveSession = resolveSession;
        this.jetTypeParameter = jetTypeParameter;
        this.variance = jetTypeParameter.getVariance();
        this.containingDeclaration = containingDeclaration;
        this.index = index;
        this.name = jetTypeParameter.getNameAsName();
        this.reified = jetTypeParameter.hasModifier(JetTokens.REIFIED_KEYWORD);

        StorageManager storageManager = resolveSession.getStorageManager();
        this.typeConstructor = storageManager.createLazyValue(new Computable<TypeConstructor>() {
            @Override
            public TypeConstructor compute() {
                return createTypeConstructor();
            }
        });
        this.defaultType = storageManager.createLazyValue(new Computable<JetType>() {
            @Override
            public JetType compute() {
                return createDefaultType();
            }
        });
        this.upperBounds = storageManager.createLazyValue(new Computable<Set<JetType>>() {
            @Override
            public Set<JetType> compute() {
                return resolveUpperBounds();
            }
        });
        this.upperBoundsAsType = storageManager.createLazyValue(new Computable<JetType>() {
            @Override
            public JetType compute() {
                return computeUpperBoundsAsType();
            }
        });
    }

    @Override
    public boolean isReified() {
        return reified;
    }

    @Override
    public Variance getVariance() {
        return variance;
    }

    @NotNull
    @Override
    public Set<JetType> getUpperBounds() {
        return upperBounds.get();
    }

    @NotNull
    private Set<JetType> resolveUpperBounds() {
        Set<JetType> upperBounds = Sets.newLinkedHashSet();

        JetTypeParameter jetTypeParameter = this.jetTypeParameter;

        resolveUpperBoundsFromWhereClause(upperBounds, false);

        JetTypeReference extendsBound = jetTypeParameter.getExtendsBound();
        if (extendsBound != null) {
            upperBounds.add(resolveBoundType(extendsBound));
        }

        if (upperBounds.isEmpty()) {
            upperBounds.add(KotlinBuiltIns.getInstance().getDefaultBound());
        }

        return upperBounds;
    }

    private void resolveUpperBoundsFromWhereClause(Set<JetType> upperBounds, boolean forClassObject) {
        JetClassOrObject classOrObject = PsiTreeUtil.getParentOfType(jetTypeParameter, JetClassOrObject.class);
        if (classOrObject instanceof JetClass) {
            JetClass jetClass = (JetClass) classOrObject;
            for (JetTypeConstraint jetTypeConstraint : jetClass.getTypeConstraints()) {
                if (jetTypeConstraint.isClassObjectContraint() != forClassObject) continue;

                JetSimpleNameExpression constrainedParameterName = jetTypeConstraint.getSubjectTypeParameterName();
                if (constrainedParameterName != null) {
                    if (name.equals(constrainedParameterName.getReferencedNameAsName())) {

                        JetTypeReference boundTypeReference = jetTypeConstraint.getBoundTypeReference();
                        if (boundTypeReference != null) {
                            upperBounds.add(resolveBoundType(boundTypeReference));
                        }
                    }
                }
            }
        }

    }

    private JetType resolveBoundType(@NotNull JetTypeReference boundTypeReference) {
        return resolveSession.getInjector().getTypeResolver()
                    .resolveType(containingDeclaration.getScopeForClassHeaderResolution(), boundTypeReference,
                                 resolveSession.getTrace(), false);
    }

    @NotNull
    @Override
    public JetType getUpperBoundsAsType() {
        return upperBoundsAsType.get();
    }

    @NotNull
    private JetType computeUpperBoundsAsType() {
        Set<JetType> upperBounds = getUpperBounds();
        assert upperBounds.size() > 0 : "Upper bound list is empty in " + getName();
        JetType upperBoundsAsType = TypeUtils.intersect(JetTypeChecker.INSTANCE, upperBounds);
        if (upperBoundsAsType == null) {
            upperBoundsAsType = KotlinBuiltIns.getInstance().getNothingType();
        }
        return upperBoundsAsType;
    }

    @NotNull
    @Override
    public Set<JetType> getLowerBounds() {
        return Collections.singleton(getLowerBoundsAsType());
    }

    @NotNull
    @Override
    public JetType getLowerBoundsAsType() {
        return KotlinBuiltIns.getInstance().getNothingType();
    }

    @NotNull
    @Override
    public TypeConstructor getTypeConstructor() {
        return typeConstructor.get();
    }

    @NotNull
    private TypeConstructor createTypeConstructor() {
        return new TypeConstructor() {
            @NotNull
            @Override
            public Collection<JetType> getSupertypes() {
                return LazyTypeParameterDescriptor.this.getUpperBounds();
            }

            @NotNull
            @Override
            public List<TypeParameterDescriptor> getParameters() {
                return Collections.emptyList();
            }

            @Override
            public boolean isSealed() {
                return false;
            }

            @Override
            public ClassifierDescriptor getDeclarationDescriptor() {
                return LazyTypeParameterDescriptor.this;
            }

            @Override
            public List<AnnotationDescriptor> getAnnotations() {
                return LazyTypeParameterDescriptor.this.getAnnotations();
            }

            @Override
            public String toString() {
                return getName().toString();
            }
        };
    }

    @NotNull
    @Override
    public JetType getDefaultType() {
        return defaultType.get();
    }

    @NotNull
    private JetType createDefaultType() {
        return new JetTypeImpl(getTypeConstructor(), new LazyScopeAdapter(new RecursionIntolerantLazyValue<JetScope>() {
                        @Override
                        protected JetScope compute() {
                            return getUpperBoundsAsType().getMemberScope();
                        }
                    }));
    }

    @Override
    public JetType getClassObjectType() {
        return null;
    }

    @Override
    public boolean isClassObjectAValue() {
        return false;
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
    @Deprecated
    public TypeParameterDescriptor substitute(@NotNull TypeSubstitutor substitutor) {
        throw new UnsupportedOperationException("Don't call substitute() on type parameters");
    }

    @Override
    public <R, D> R accept(DeclarationDescriptorVisitor<R, D> visitor, D data) {
        return visitor.visitTypeParameterDescriptor(this, data);
    }

    @Override
    public void acceptVoid(DeclarationDescriptorVisitor<Void, Void> visitor) {
        visitor.visitTypeParameterDescriptor(this, null);
    }

    @Override
    public int getIndex() {
        return index;
    }

    @Override
    public List<AnnotationDescriptor> getAnnotations() {
        return Collections.emptyList(); // TODO
    }

    @NotNull
    @Override
    public Name getName() {
        return name;
    }

    @Override
    public String toString() {
        return getName().toString();
    }

    @Override
    public void forceResolveAllContents() {
        getAnnotations();
        getClassObjectType();
        getContainingDeclaration();
        getDefaultType();
        getIndex();
        getLowerBounds();
        getLowerBoundsAsType();
        getOriginal();
        getTypeConstructor();
        getUpperBounds();
        getUpperBoundsAsType();
        getVariance();
    }
}
