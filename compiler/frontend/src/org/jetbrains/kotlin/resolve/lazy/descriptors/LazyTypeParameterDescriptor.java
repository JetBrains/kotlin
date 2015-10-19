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
import org.jetbrains.kotlin.descriptors.impl.AbstractLazyTypeParameterDescriptor;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.lazy.ForceResolveUtil;
import org.jetbrains.kotlin.resolve.lazy.LazyClassContext;
import org.jetbrains.kotlin.resolve.lazy.LazyEntity;
import org.jetbrains.kotlin.resolve.source.KotlinSourceElementKt;
import org.jetbrains.kotlin.types.KtType;

import java.util.Set;

public class LazyTypeParameterDescriptor extends AbstractLazyTypeParameterDescriptor implements LazyEntity {
    private final LazyClassContext c;

    private final KtTypeParameter jetTypeParameter;

    public LazyTypeParameterDescriptor(
            @NotNull LazyClassContext c,
            @NotNull LazyClassDescriptor containingDeclaration,
            @NotNull KtTypeParameter jetTypeParameter,
            int index) {
        super(
                c.getStorageManager(),
                containingDeclaration,
                jetTypeParameter.getNameAsSafeName(),
                jetTypeParameter.getVariance(),
                jetTypeParameter.hasModifier(KtTokens.REIFIED_KEYWORD),
                index,
                KotlinSourceElementKt.toSourceElement(jetTypeParameter)
        );
        this.c = c;
        this.jetTypeParameter = jetTypeParameter;

        this.c.getTrace().record(BindingContext.TYPE_PARAMETER, jetTypeParameter, this);
    }

    @NotNull
    @Override
    protected Set<KtType> resolveUpperBounds() {
        Set<KtType> upperBounds = Sets.newLinkedHashSet();

        KtTypeParameter jetTypeParameter = this.jetTypeParameter;

        KtTypeReference extendsBound = jetTypeParameter.getExtendsBound();
        if (extendsBound != null) {
            KtType boundType = c.getDescriptorResolver().resolveTypeParameterExtendsBound(
                    this, extendsBound, getContainingDeclaration().getScopeForClassHeaderResolution(), c.getTrace());
            upperBounds.add(boundType);
        }

        resolveUpperBoundsFromWhereClause(upperBounds);

        if (upperBounds.isEmpty()) {
            upperBounds.add(c.getModuleDescriptor().getBuiltIns().getDefaultBound());
        }

        return upperBounds;
    }

    private void resolveUpperBoundsFromWhereClause(Set<KtType> upperBounds) {
        KtClassOrObject classOrObject = KtStubbedPsiUtil.getPsiOrStubParent(jetTypeParameter, KtClassOrObject.class, true);
        if (classOrObject instanceof KtClass) {
            KtClass ktClass = (KtClass) classOrObject;
            for (KtTypeConstraint jetTypeConstraint : ktClass.getTypeConstraints()) {
                KtSimpleNameExpression constrainedParameterName = jetTypeConstraint.getSubjectTypeParameterName();
                if (constrainedParameterName != null) {
                    if (getName().equals(constrainedParameterName.getReferencedNameAsName())) {
                        c.getTrace().record(BindingContext.REFERENCE_TARGET, constrainedParameterName, this);

                        KtTypeReference boundTypeReference = jetTypeConstraint.getBoundTypeReference();
                        if (boundTypeReference != null) {
                            KtType boundType = resolveBoundType(boundTypeReference);
                            upperBounds.add(boundType);
                        }
                    }
                }
            }
        }

    }

    private KtType resolveBoundType(@NotNull KtTypeReference boundTypeReference) {
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
