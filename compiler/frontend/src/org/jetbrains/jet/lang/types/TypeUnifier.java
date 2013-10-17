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

package org.jetbrains.jet.lang.types;

import com.google.common.base.Predicate;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class TypeUnifier {

    public interface UnificationResult {
        boolean isSuccess();

        @NotNull
        Map<TypeConstructor, TypeProjection> getSubstitution();
    }

    /**
     * Finds a substitution S that turns {@code projectWithVariables} to {@code knownProjection}.
     *
     * Example:
     *      known = List<String>
     *      withVariables = List<X>
     *      variables = {X}
     *
     *      result = X -> String
     *
     * Only types accepted by {@code isVariable} are considered variables.
     */
    @NotNull
    public static UnificationResult unify(
            @NotNull TypeProjection knownProjection,
            @NotNull TypeProjection projectWithVariables,
            @NotNull Predicate<TypeConstructor> isVariable
    ) {
        UnificationResultImpl result = new UnificationResultImpl();
        doUnify(knownProjection, projectWithVariables, isVariable, result);
        return result;
    }

    private static void doUnify(
            TypeProjection knownProjection,
            TypeProjection projectWithVariables,
            Predicate<TypeConstructor> isVariable,
            UnificationResultImpl result
    ) {
        JetType known = knownProjection.getType();
        JetType withVariables = projectWithVariables.getType();

        // in Foo ~ in X  =>  Foo ~ X
        Variance knownProjectionKind = knownProjection.getProjectionKind();
        Variance withVariablesProjectionKind = projectWithVariables.getProjectionKind();
        if (knownProjectionKind == withVariablesProjectionKind && knownProjectionKind != Variance.INVARIANT) {
            doUnify(new TypeProjectionImpl(known), new TypeProjectionImpl(withVariables), isVariable, result);
            return;
        }

        // Foo? ~ X?  =>  Foo ~ X
        if (known.isNullable() && withVariables.isNullable()) {
            doUnify(
                    new TypeProjectionImpl(knownProjectionKind, TypeUtils.makeNotNullable(known)),
                    new TypeProjectionImpl(withVariablesProjectionKind, TypeUtils.makeNotNullable(withVariables)),
                    isVariable,
                    result
            );
            return;
        }

        // in Foo ~ out X  => fail
        // in Foo ~ X  =>  may be OK
        if (knownProjectionKind != withVariablesProjectionKind && withVariablesProjectionKind != Variance.INVARIANT) {
            result.fail();
            return;
        }

        // Foo ~ X? => fail
        if (!known.isNullable() && withVariables.isNullable()) {
            result.fail();
            return;
        }

        // Foo ~ X  =>  x |-> Foo
        TypeConstructor maybeVariable = withVariables.getConstructor();
        if (isVariable.apply(maybeVariable)) {
            result.put(maybeVariable, new TypeProjectionImpl(knownProjectionKind, known));
            return;
        }

        // Foo? ~ Foo || in Foo ~ Foo || Foo ~ Bar
        boolean structuralMismatch = known.isNullable() != withVariables.isNullable()
                || knownProjectionKind != withVariablesProjectionKind
                || !known.getConstructor().equals(withVariables.getConstructor());
        if (structuralMismatch) {
            result.fail();
            return;
        }

        // Foo<A> ~ Foo<B, C>
        if (known.getArguments().size() != withVariables.getArguments().size()) {
            result.fail();
            return;
        }

        // Foo ~ Foo
        if (known.getArguments().isEmpty()) {
            return;
        }

        // Foo<...> ~ Foo<...>
        List<TypeProjection> knownArguments = known.getArguments();
        List<TypeProjection> withVariablesArguments = withVariables.getArguments();
        for (int i = 0; i < knownArguments.size(); i++) {
            TypeProjection knownArg = knownArguments.get(i);
            TypeProjection withVariablesArg = withVariablesArguments.get(i);

            doUnify(knownArg, withVariablesArg, isVariable, result);
        }
    }

    private static class UnificationResultImpl implements UnificationResult {
        private boolean success = true;
        private final Map<TypeConstructor, TypeProjection> substitution = Maps.newHashMapWithExpectedSize(1);
        private final Set<TypeConstructor> failedVariables = Sets.newHashSetWithExpectedSize(0);

        @Override
        public boolean isSuccess() {
            return success;
        }

        public void fail() {
            success = false;
        }

        @Override
        @NotNull
        public Map<TypeConstructor, TypeProjection> getSubstitution() {
            return substitution;
        }

        public void put(TypeConstructor key, TypeProjection value) {
            if (failedVariables.contains(key)) return;

            TypeProjection oldValue = substitution.put(key, value);
            if (oldValue != null && !oldValue.equals(value)) {
                substitution.remove(key);
                failedVariables.add(key);
                fail();
            }
        }
    }
}
