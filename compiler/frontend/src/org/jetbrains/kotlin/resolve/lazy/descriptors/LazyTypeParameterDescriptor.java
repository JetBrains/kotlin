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

package org.jetbrains.kotlin.resolve.lazy.descriptors;

import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.resolve.lazy.LazyClassContext;
import org.jetbrains.kotlin.descriptors.impl.AbstractLazyTypeParameterDescriptor;
import org.jetbrains.kotlin.lexer.JetTokens;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.DescriptorResolver;
import org.jetbrains.kotlin.resolve.lazy.ForceResolveUtil;
import org.jetbrains.kotlin.resolve.lazy.LazyEntity;
import org.jetbrains.kotlin.types.JetType;

import java.util.Set;

import static org.jetbrains.kotlin.resolve.source.SourcePackage.toSourceElement;

public class LazyTypeParameterDescriptor extends AbstractLazyTypeParameterDescriptor implements LazyEntity {
    private final LazyClassContext c;

    private final JetTypeParameter jetTypeParameter;

    public LazyTypeParameterDescriptor(
            @NotNull LazyClassContext c,
            @NotNull LazyClassDescriptor containingDeclaration,
            @NotNull JetTypeParameter jetTypeParameter,
            int index) {
        super(
                c.getStorageManager(),
                containingDeclaration,
                jetTypeParameter.getNameAsSafeName(),
                jetTypeParameter.getVariance(),
                jetTypeParameter.hasModifier(JetTokens.REIFIED_KEYWORD),
                index,
                toSourceElement(jetTypeParameter)
        );
        this.c = c;
        this.jetTypeParameter = jetTypeParameter;

        this.c.getTrace().record(BindingContext.TYPE_PARAMETER, jetTypeParameter, this);
    }

    @NotNull
    @Override
    protected Set<JetType> resolveUpperBounds() {
        Set<JetType> upperBounds = Sets.newLinkedHashSet();

        JetTypeParameter jetTypeParameter = this.jetTypeParameter;

        JetTypeReference extendsBound = jetTypeParameter.getExtendsBound();
        if (extendsBound != null) {
            upperBounds.add(resolveBoundType(extendsBound));
        }

        resolveUpperBoundsFromWhereClause(upperBounds);

        if (upperBounds.isEmpty()) {
            upperBounds.add(c.getModuleDescriptor().getBuiltIns().getDefaultBound());
        }

        return upperBounds;
    }

    private void resolveUpperBoundsFromWhereClause(Set<JetType> upperBounds) {
        JetClassOrObject classOrObject = JetStubbedPsiUtil.getPsiOrStubParent(jetTypeParameter, JetClassOrObject.class, true);
        if (classOrObject instanceof JetClass) {
            JetClass jetClass = (JetClass) classOrObject;
            for (JetTypeConstraint jetTypeConstraint : jetClass.getTypeConstraints()) {
                DescriptorResolver.reportUnsupportedDefaultObjectConstraint(c.getTrace(), jetTypeConstraint);

                JetSimpleNameExpression constrainedParameterName = jetTypeConstraint.getSubjectTypeParameterName();
                if (constrainedParameterName != null) {
                    if (getName().equals(constrainedParameterName.getReferencedNameAsName())) {
                        c.getTrace().record(BindingContext.REFERENCE_TARGET, constrainedParameterName, this);

                        JetTypeReference boundTypeReference = jetTypeConstraint.getBoundTypeReference();
                        if (boundTypeReference != null) {
                            JetType boundType = resolveBoundType(boundTypeReference);
                            if (!jetTypeConstraint.isDefaultObjectConstraint()) {
                                upperBounds.add(boundType);
                            }
                        }
                    }
                }
            }
        }

    }

    private JetType resolveBoundType(@NotNull JetTypeReference boundTypeReference) {
        return c.getTypeResolver()
                    .resolveType(getContainingDeclaration().getScopeForClassHeaderResolution(), boundTypeReference,
                                 c.getTrace(), false);
    }

    @NotNull
    @Override
    public LazyClassDescriptor getContainingDeclaration() {
        return (LazyClassDescriptor) super.getContainingDeclaration();
    }

    @Override
    public void forceResolveAllContents() {
        ForceResolveUtil.forceResolveAllContents(getAnnotations());
        ForceResolveUtil.forceResolveAllContents(getClassObjectType());
        getContainingDeclaration();
        getDefaultType();
        getIndex();
        ForceResolveUtil.forceResolveAllContents(getLowerBounds());
        getOriginal();
        ForceResolveUtil.forceResolveAllContents(getTypeConstructor());
        ForceResolveUtil.forceResolveAllContents(getUpperBounds());
        getUpperBoundsAsType();
        getVariance();
    }
}
