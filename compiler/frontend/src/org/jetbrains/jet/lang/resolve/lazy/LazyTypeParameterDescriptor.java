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

import com.google.common.collect.Sets;
import com.intellij.psi.SmartPsiElementPointer;
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
import org.jetbrains.jet.lang.types.lang.JetStandardClasses;
import org.jetbrains.jet.util.lazy.LazyValue;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author abreslav
 */
public class LazyTypeParameterDescriptor implements TypeParameterDescriptor {
    private final ResolveSession resolveSession;

    private final SmartPsiElementPointer<JetTypeParameter> jetTypeParameterSmartPsiElementPointer;
    private final Variance variance;
    private final int index;
    private final LazyClassDescriptor containingDeclaration;
    private final Name name;

    private TypeConstructor typeConstructor;
    private JetType defaultType;

    private Set<JetType> upperBounds;
    private JetType upperBoundsAsType;

    private Set<JetType> classObjectBounds;
    private JetType classObjectBoundsAsType;

    public LazyTypeParameterDescriptor(
            @NotNull ResolveSession resolveSession,
            @NotNull LazyClassDescriptor containingDeclaration,
            @NotNull JetTypeParameter jetTypeParameter,
            int index) {
        this.resolveSession = resolveSession;
        // TODO: different smart pointer implementations in IDE and compiler
        this.jetTypeParameterSmartPsiElementPointer = new IdentitySmartPointer<JetTypeParameter>(jetTypeParameter);
        this.variance = jetTypeParameter.getVariance();
        this.containingDeclaration = containingDeclaration;
        this.index = index;
        this.name = jetTypeParameter.getNameAsName();
    }

    @Override
    public boolean isReified() {
        return false;
    }

    @Override
    public Variance getVariance() {
        return variance;
    }

    @NotNull
    @Override
    public Set<JetType> getUpperBounds() {
        if (upperBounds == null) {
            upperBounds = Sets.newLinkedHashSet();

            JetTypeParameter jetTypeParameter = getElement();

            resolveUpperBoundsFromWhereClause(upperBounds, false);

            JetTypeReference extendsBound = jetTypeParameter.getExtendsBound();
            if (extendsBound != null) {
                upperBounds.add(resolveBoundType(extendsBound));
            }

            if (upperBounds.isEmpty()) {
                upperBounds.add(JetStandardClasses.getDefaultBound());
            }
        }
        return upperBounds;
    }

    private JetTypeParameter resolveUpperBoundsFromWhereClause(Set<JetType> upperBounds, boolean forClassObject) {
        JetTypeParameter jetTypeParameter = getElement();

        JetClassOrObject classOrObject = PsiTreeUtil.getParentOfType(jetTypeParameter, JetClassOrObject.class);
        if (classOrObject instanceof JetClass) {
            JetClass jetClass = (JetClass) classOrObject;
            for (JetTypeConstraint jetTypeConstraint : jetClass.getTypeConstaints()) {
                if (jetTypeConstraint.isClassObjectContraint() == forClassObject) continue;

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
        return jetTypeParameter;
    }

    private JetTypeParameter getElement() {
        JetTypeParameter jetTypeParameter = jetTypeParameterSmartPsiElementPointer.getElement();
        if (jetTypeParameter == null) {
            throw new IllegalStateException("Psi element not found for type parameter: " + this);
        }
        return jetTypeParameter;
    }

    private JetType resolveBoundType(@NotNull JetTypeReference boundTypeReference) {
        return resolveSession.getTypeResolver()
                    .resolveType(containingDeclaration.getScopeForClassHeaderResolution(), boundTypeReference,
                                 resolveSession.getTrace(), false);
    }

    @NotNull
    @Override
    public JetType getUpperBoundsAsType() {
        if (upperBoundsAsType == null) {
            Set<JetType> upperBounds = getUpperBounds();
            assert upperBounds.size() > 0 : "Upper bound list is empty in " + getName();
            upperBoundsAsType = TypeUtils.intersect(JetTypeChecker.INSTANCE, upperBounds);
            if (upperBoundsAsType == null) {
                upperBoundsAsType = JetStandardClasses.getNothingType();
            }
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
        return JetStandardClasses.getNothingType();
    }

    @NotNull
    @Override
    public TypeConstructor getTypeConstructor() {
        if (typeConstructor == null) {
            typeConstructor = new TypeConstructor() {
                @NotNull
                @Override
                public Collection<? extends JetType> getSupertypes() {
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
        return typeConstructor;
    }

    @NotNull
    @Override
    public JetType getDefaultType() {
        if (defaultType == null) {
            defaultType = new JetTypeImpl(getTypeConstructor(), new LazyScopeAdapter(new LazyValue<JetScope>() {
                @Override
                protected JetScope compute() {
                    return getUpperBoundsAsType().getMemberScope();
                }
            }));
        }
        return defaultType;
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

    @Override
    public DeclarationDescriptor getContainingDeclaration() {
        return containingDeclaration;
    }

    @NotNull
    @Override
    @Deprecated
    public TypeParameterDescriptor substitute(TypeSubstitutor substitutor) {
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
}
