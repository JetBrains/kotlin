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

import kotlin.collections.CollectionsKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.descriptors.impl.AbstractLazyTypeParameterDescriptor;
import org.jetbrains.kotlin.diagnostics.Errors;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.lazy.ForceResolveUtil;
import org.jetbrains.kotlin.resolve.lazy.LazyClassContext;
import org.jetbrains.kotlin.resolve.lazy.LazyEntity;
import org.jetbrains.kotlin.resolve.source.KotlinSourceElementKt;
import org.jetbrains.kotlin.types.KotlinType;
import org.jetbrains.kotlin.types.KotlinTypeKt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
                KotlinSourceElementKt.toSourceElement(typeParameter),
                c.getSupertypeLoopChecker());
        this.c = c;
        this.typeParameter = typeParameter;

        this.c.getTrace().record(BindingContext.TYPE_PARAMETER, typeParameter, this);
    }

    @Override
    protected void reportSupertypeLoopError(@NotNull KotlinType type) {
        for (KtTypeReference typeReference : getAllUpperBounds()) {
            if (resolveBoundType(typeReference).getConstructor().equals(type.getConstructor())) {
                c.getTrace().report(Errors.CYCLIC_GENERIC_UPPER_BOUND.on(typeReference));
                return;
            }
        }
    }

    @NotNull
    @Override
    protected List<KotlinType> resolveUpperBounds() {
        List<KotlinType> upperBounds = new ArrayList<>(1);

        for (KtTypeReference typeReference : getAllUpperBounds()) {
            KotlinType resolvedType = resolveBoundType(typeReference);
            if (!KotlinTypeKt.isError(resolvedType)) {
                upperBounds.add(resolvedType);
            }
        }

        if (upperBounds.isEmpty()) {
            upperBounds.add(c.getModuleDescriptor().getBuiltIns().getDefaultBound());
        }

        return upperBounds;
    }

    private Collection<KtTypeReference> getAllUpperBounds() {
        return CollectionsKt.plus(
                typeParameter.getExtendsBound() != null
                ? Collections.singletonList(typeParameter.getExtendsBound())
                : Collections.emptyList(),
                getUpperBoundsFromWhereClause()
        );
    }

    private Collection<KtTypeReference> getUpperBoundsFromWhereClause() {
        Collection<KtTypeReference> result = new ArrayList<>();

        KtClassOrObject classOrObject = KtStubbedPsiUtil.getPsiOrStubParent(typeParameter, KtClassOrObject.class, true);
        if (classOrObject instanceof KtClass) {
            for (KtTypeConstraint typeConstraint : classOrObject.getTypeConstraints()) {
                KtSimpleNameExpression constrainedParameterName = typeConstraint.getSubjectTypeParameterName();
                if (constrainedParameterName != null) {
                    if (getName().equals(constrainedParameterName.getReferencedNameAsName())) {
                        c.getTrace().record(BindingContext.REFERENCE_TARGET, constrainedParameterName, this);

                        KtTypeReference boundTypeReference = typeConstraint.getBoundTypeReference();
                        if (boundTypeReference != null) {
                            result.add(boundTypeReference);
                        }
                    }
                }
            }
        }

        return result;
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
        getOriginal();
        ForceResolveUtil.forceResolveAllContents(getTypeConstructor());
        ForceResolveUtil.forceResolveAllContents(getUpperBounds());
        getVariance();
    }
}
