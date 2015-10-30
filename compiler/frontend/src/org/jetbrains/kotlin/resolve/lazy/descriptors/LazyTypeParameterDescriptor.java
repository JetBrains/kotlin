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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.descriptors.impl.AbstractLazyTypeParameterDescriptor;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.lazy.ForceResolveUtil;
import org.jetbrains.kotlin.resolve.lazy.LazyClassContext;
import org.jetbrains.kotlin.resolve.lazy.LazyEntity;
import org.jetbrains.kotlin.resolve.source.KotlinSourceElementKt;
import org.jetbrains.kotlin.types.KotlinType;

import java.util.ArrayList;
import java.util.List;

public class LazyTypeParameterDescriptor extends AbstractLazyTypeParameterDescriptor implements LazyEntity {
    private final LazyClassContext c;
    private final KtTypeParameter typeParameter;

    public LazyTypeParameterDescriptor(
            @NotNull LazyClassContext c,
            @NotNull LazyClassDescriptor containingDeclaration,
            @NotNull KtTypeParameter typeParameter,
            int index
    ) {
        super(
                c.getStorageManager(),
                containingDeclaration,
                typeParameter.getNameAsSafeName(),
                typeParameter.getVariance(),
                typeParameter.hasModifier(KtTokens.REIFIED_KEYWORD),
                index,
                KotlinSourceElementKt.toSourceElement(typeParameter)
        );
        this.c = c;
        this.typeParameter = typeParameter;

        this.c.getTrace().record(BindingContext.TYPE_PARAMETER, typeParameter, this);
    }

    @NotNull
    @Override
    protected List<KotlinType> resolveUpperBounds() {
        List<KotlinType> upperBounds = new ArrayList<KotlinType>(1);

        KtTypeReference extendsBound = typeParameter.getExtendsBound();
        if (extendsBound != null) {
            KotlinType boundType = c.getDescriptorResolver().resolveTypeParameterExtendsBound(
                    this, extendsBound, getContainingDeclaration().getScopeForClassHeaderResolution(), c.getTrace()
            );
            upperBounds.add(boundType);
        }

        resolveUpperBoundsFromWhereClause(upperBounds);

        if (upperBounds.isEmpty()) {
            upperBounds.add(c.getModuleDescriptor().getBuiltIns().getDefaultBound());
        }

        return upperBounds;
    }

    private void resolveUpperBoundsFromWhereClause(@NotNull List<KotlinType> upperBounds) {
        KtClassOrObject classOrObject = KtStubbedPsiUtil.getPsiOrStubParent(typeParameter, KtClassOrObject.class, true);
        if (classOrObject instanceof KtClass) {
            for (KtTypeConstraint typeConstraint : classOrObject.getTypeConstraints()) {
                KtSimpleNameExpression constrainedParameterName = typeConstraint.getSubjectTypeParameterName();
                if (constrainedParameterName != null) {
                    if (getName().equals(constrainedParameterName.getReferencedNameAsName())) {
                        c.getTrace().record(BindingContext.REFERENCE_TARGET, constrainedParameterName, this);

                        KtTypeReference boundTypeReference = typeConstraint.getBoundTypeReference();
                        if (boundTypeReference != null) {
                            upperBounds.add(resolveBoundType(boundTypeReference));
                        }
                    }
                }
            }
        }
    }

    @NotNull
    private KotlinType resolveBoundType(@NotNull KtTypeReference boundTypeReference) {
        return c.getTypeResolver().resolveType(
                getContainingDeclaration().getScopeForClassHeaderResolution(), boundTypeReference, c.getTrace(), false
        );
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
        getVariance();
    }
}
