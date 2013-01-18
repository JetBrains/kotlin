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

package org.jetbrains.jet.lang.resolve.calls.inference;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeSubstitutor;
import org.jetbrains.jet.lang.types.Variance;

import java.util.Set;

public interface ConstraintSystem {

    /**
     * Registers a variable in a constraint system.
     */
    void registerTypeVariable(@NotNull TypeParameterDescriptor typeVariable, @NotNull Variance positionVariance);

    /**
     * Returns a set of all registered type variables.
     */
    @NotNull
    Set<TypeParameterDescriptor> getTypeVariables();

    /**
     * Adds a constraint that the constraining type is a subtype of the subject type.<p/>
     * Asserts that only subject type may contain registered type variables. <p/>
     *
     * For example, for {@code "fun <T> id(t: T) {}"} to infer <tt>T</tt> in invocation <tt>"id(1)"</tt>
     * should be generated a constraint <tt>"Int is a subtype of T"</tt> where T is a subject type, and Int is a constraining type.
     */
    void addSubtypeConstraint(@Nullable JetType constrainingType, @NotNull JetType subjectType, @NotNull ConstraintPosition constraintPosition);

    /**
     * Adds a constraint that the constraining type is a supertype of the subject type. <p/>
     * Asserts that only subject type may contain registered type variables. <p/>
     *
     * For example, for {@code "fun <T> create() : T"} to infer <tt>T</tt> in invocation <tt>"val i: Int = create()"</tt>
     * should be generated a constraint <tt>"Int is a supertype of T"</tt> where T is a subject type, and Int is a constraining type.
     */
    void addSupertypeConstraint(@Nullable JetType constrainingType, @NotNull JetType subjectType, @NotNull ConstraintPosition constraintPosition);

    /**
     * Returns <tt>true</tt> if constraint system has a solution (has no contradiction and has enough information to infer each registered type variable).
     */
    boolean isSuccessful();

    /**
     * Return <tt>true</tt> if constraint system has no contradiction (it can be not successful because of the lack of information for a type variable).
     */
    boolean hasContradiction();

    /**
     * Returns <tt>true</tt> if type constraints for some type variable are contradicting. <p/>
     *
     * For example, for <pre>fun &lt;R&gt; foo(r: R, t: java.util.List&lt;R&gt;) {}</pre> in invocation <tt>foo(1, arrayList("s"))</tt>
     * type variable <tt>R</tt> has two conflicting constraints: <p/>
     * - <tt>"R is a supertype of Int"</tt> <p/>
     * - <tt>"List&lt;R&gt; is a supertype of List&lt;String&gt;"</tt> which leads to <tt>"R is equal to String"</tt>
     */
    boolean hasConflictingConstraints();

    /**
     * Returns <tt>true</tt> if there is no information for some registered type variable.
     *
     * For example, for <pre>fun &lt;E&gt; newList()</pre> in invocation <tt>"val nl = newList()"</tt>
     * there is no information to infer type variable <tt>E</tt>.
     */
    boolean hasUnknownParameters();

    /**
     * Returns <tt>true</tt> if some constraint cannot be processed because of type constructor mismatch.
     *
     * For example, for <pre>fun &lt;R&gt; foo(t: List&lt;R&gt;) {}</pre> in invocation <tt>foo(hashSet("s"))</tt>
     * there is type constructor mismatch: <tt>"HashSet&lt;String&gt; cannot be a subtype of List&lt;R&gt;"</tt>.
     */
    boolean hasTypeConstructorMismatch();

    /**
     * Returns <tt>true</tt> if there is type constructor mismatch error at a specific {@code constraintPosition}.
     *
     * For example, for <pre>fun &lt;R&gt; foo(t: List&lt;R&gt;) {}</pre> in invocation <tt>foo(hashSet("s"))</tt>
     * there is type constructor mismatch: <tt>"HashSet&lt;String&gt; cannot be a subtype of List&lt;R&gt;"</tt>
     * at a constraint position {@code ConstraintPosition.getValueParameterPosition(0)}.
     */
    boolean hasTypeConstructorMismatchAt(@NotNull ConstraintPosition constraintPosition);

    /**
     * Returns <tt>true</tt> if there is type constructor mismatch only in {@link ConstraintPosition.EXPECTED_TYPE_POSITION}.
     */
    boolean hasExpectedTypeMismatch();

    /**
     * Returns <tt>true</tt> if there is an error in constraining types. <p/>
     * Is used not to generate type inference error if there was one in argument types.
     */
    boolean hasErrorInConstrainingTypes();

    /**
     * Returns the resulting type constraints of solving the constraint system for specific type variable. <p/>
     * Returns null if the type variable was not registered.
     */
    @Nullable
    TypeConstraints getTypeConstraints(@NotNull TypeParameterDescriptor typeVariable);

    /**
     * Returns a result of solving the constraint system (mapping from the type variable to the resulting type projection). <p/>
     * In the resulting substitution should be concerned: <p/>
     * - type constraints <p/>
     * - variance of the type variable  // not implemented yet <p/>
     * - type parameter bounds (that can bind type variables with each other). // not implemented yet
     */
    @NotNull
    TypeSubstitutor getResultingSubstitutor();

    /**
     * Returns a current result of solving the constraint system (mapping from the type variable to the resulting type projection).
     * If there is no information for type parameter, returns type projection for DONT_CARE type.
     */
    @NotNull
    TypeSubstitutor getCurrentSubstitutor();

    ConstraintSystem copy();
}
