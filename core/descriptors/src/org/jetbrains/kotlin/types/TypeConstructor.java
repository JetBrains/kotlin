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

package org.jetbrains.kotlin.types;

import kotlin.annotations.jvm.ReadOnly;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor;
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor;
import org.jetbrains.kotlin.types.checker.KotlinTypeRefiner;
import org.jetbrains.kotlin.types.model.TypeConstructorMarker;

import java.util.Collection;
import java.util.List;

public interface TypeConstructor extends TypeConstructorMarker {
    /**
     * It may differ from ClassDescriptor.declaredParameters if the class is inner, in such case
     * it also contains additional parameters from outer declarations.
     *
     * @return list of parameters for type constructor, both from current declaration and the outer one
     */
    @NotNull
    @ReadOnly
    List<TypeParameterDescriptor> getParameters();

    @NotNull
    @ReadOnly
    Collection<KotlinType> getSupertypes();

    /**
     * Cannot have subtypes.
     */
    boolean isFinal();

    /**
     * If the type is non-denotable, it can't be written in code directly, it only can appear internally inside a type checker.
     * Examples: intersection type or number value type.
     */
    boolean isDenotable();

    @Nullable
    ClassifierDescriptor getDeclarationDescriptor();

    @NotNull
    KotlinBuiltIns getBuiltIns();

    /**
     * Returns TypeConstructor, refined with passed refined, if that makes sense for this specific typeConstructor
     *
     * Contract:
     * - returned TypeConstructor has refined supertypes, i.e. it has correct supertypes resolved as if
     *   we were looking at them from refiner's module
     * - IT DOES NOT ADD PLATFORM DECLARED SUPERTYPES!!!!!!!!
     * - all other similar sources of KotlinTypes/Descriptors should return refined instances as well
     *
     * That method is part of internal refinement infrastructure, so IT SHOULD NOT BE CALLED from anywhere except
     *   methods from refinement (like methods of KotlinTypeRefinerImpl or KotlinType.refine
     *
     * Implementation notice:
     * - the most interesting part happens in 'AbstractTypeConstructor': it returns 'ModuleViewTypeConstructor', which
     *   will refine supertypes when queried for them
     * - also, there are several typeConstructors, which do not inherit AbstractTypeConstructor, but have some component
     *   types/descriptors (e.g. IntersectionTypeConstructor) -- they refine their content manually by recursing using refiner
     * - finally, most special typeConstructors have no meaningful refinement and return null (i.e. UninferredTypeParameterConstructor)
     */
    @TypeRefinement
    @NotNull
    TypeConstructor refine(@NotNull KotlinTypeRefiner kotlinTypeRefiner);
}
