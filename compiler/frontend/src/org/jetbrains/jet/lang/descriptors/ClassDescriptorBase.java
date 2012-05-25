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

package org.jetbrains.jet.lang.descriptors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.SubstitutingScope;
import org.jetbrains.jet.lang.types.*;

import java.util.List;
import java.util.Map;

/**
 * @author abreslav
 */
public abstract class ClassDescriptorBase extends MutableDeclarationDescriptor implements ClassDescriptor {
    public ClassDescriptorBase(DeclarationDescriptor containingDeclaration) {
        super(containingDeclaration);
    }

    protected abstract JetScope getScopeForMemberLookup();

    @NotNull
    @Override
    public JetScope getMemberScope(List<TypeProjection> typeArguments) {
        assert typeArguments.size() == getTypeConstructor().getParameters().size();
        if (typeArguments.isEmpty()) return getScopeForMemberLookup();

        List<TypeParameterDescriptor> typeParameters = getTypeConstructor().getParameters();
        Map<TypeConstructor, TypeProjection> substitutionContext = SubstitutionUtils.buildSubstitutionContext(typeParameters, typeArguments);

        // Unsafe substitutor is OK, because no recursion can hurt us upon a trivial substitution:
        // all the types are written explicitly in the code already, they can not get infinite.
        // One exception is *-projections, but they need to be handled separately anyways.
        TypeSubstitutor substitutor = TypeSubstitutor.createUnsafe(substitutionContext);
        return new SubstitutingScope(getScopeForMemberLookup(), substitutor);
    }

    @NotNull
    @Override
    public ClassDescriptor substitute(TypeSubstitutor substitutor) {
        if (substitutor.isEmpty()) {
            return this;
        }
        return new LazySubstitutingClassDescriptor(this, substitutor);
    }
}
