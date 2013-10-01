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

package org.jetbrains.jet.lang.resolve.lazy.descriptors;

import com.google.common.collect.Sets;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;
import org.jetbrains.jet.lang.descriptors.impl.AbstractLazyTypeParameterDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.lazy.LazyDescriptor;
import org.jetbrains.jet.lang.resolve.lazy.ResolveSession;
import org.jetbrains.jet.lang.resolve.lazy.ResolveSessionUtils;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.lexer.JetTokens;

import java.util.Set;

public class LazyTypeParameterDescriptor extends AbstractLazyTypeParameterDescriptor implements TypeParameterDescriptor, LazyDescriptor {
    private final ResolveSession resolveSession;

    private final JetTypeParameter jetTypeParameter;

    public LazyTypeParameterDescriptor(
            @NotNull ResolveSession resolveSession,
            @NotNull LazyClassDescriptor containingDeclaration,
            @NotNull JetTypeParameter jetTypeParameter,
            int index) {
        super(
                resolveSession.getStorageManager(),
                containingDeclaration,
                ResolveSessionUtils.safeNameForLazyResolve(jetTypeParameter.getNameAsName()),
                jetTypeParameter.getVariance(),
                jetTypeParameter.hasModifier(JetTokens.REIFIED_KEYWORD),
                index
        );
        this.resolveSession = resolveSession;
        this.jetTypeParameter = jetTypeParameter;

        this.resolveSession.getTrace().record(BindingContext.TYPE_PARAMETER, jetTypeParameter, this);
    }

    @NotNull
    @Override
    protected Set<JetType> resolveUpperBounds() {
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
                    if (getName().equals(constrainedParameterName.getReferencedNameAsName())) {

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
                    .resolveType(getContainingDeclaration().getScopeForClassHeaderResolution(), boundTypeReference,
                                 resolveSession.getTrace(), false);
    }

    @NotNull
    @Override
    public LazyClassDescriptor getContainingDeclaration() {
        return (LazyClassDescriptor) super.getContainingDeclaration();
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
