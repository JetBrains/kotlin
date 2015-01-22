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

package org.jetbrains.kotlin.resolve.calls.inference

import org.jetbrains.kotlin.resolve.calls.inference.constraintPosition.ConstraintPosition

public trait ConstraintSystemStatus {
    /**
     * Returns <tt>true</tt> if constraint system has a solution (has no contradiction and has enough information to infer each registered type variable).
     */
    public fun isSuccessful(): Boolean

    /**
     * Return <tt>true</tt> if constraint system has no contradiction (it can be not successful because of the lack of information for a type variable).
     */
    public fun hasContradiction(): Boolean

    /**
     * Returns <tt>true</tt> if type constraints for some type variable are contradicting. <p/>
     *
     * For example, for <pre>fun &lt;R&gt; foo(r: R, t: java.util.List&lt;R&gt;) {}</pre> in invocation <tt>foo(1, arrayList("s"))</tt>
     * type variable <tt>R</tt> has two conflicting constraints: <p/>
     * - <tt>"R is a supertype of Int"</tt> <p/>
     * - <tt>"List&lt;R&gt; is a supertype of List&lt;String&gt;"</tt> which leads to <tt>"R is equal to String"</tt>
     */
    public fun hasConflictingConstraints(): Boolean

    /**
     * Returns <tt>true</tt> if contradiction of type constraints comes from declared bounds for type parameters.
     *
     * For example, for <pre>fun &lt;R: Any&gt; foo(r: R) {}</pre> in invocation <tt>foo(null)</tt>
     * upper bounds <tt>Any</tt> for type parameter <tt>R</tt> is violated. <p/>
     *
     * It's the special case of 'hasConflictingConstraints' case.
     */
    public fun hasViolatedUpperBound(): Boolean

    /**
     * Returns <tt>true</tt> if there is no information for some registered type variable.
     *
     * For example, for <pre>fun &lt;E&gt; newList()</pre> in invocation <tt>"val nl = newList()"</tt>
     * there is no information to infer type variable <tt>E</tt>.
     */
    public fun hasUnknownParameters(): Boolean

    /**
     * Returns <tt>true</tt> if some constraint cannot be processed because of type constructor mismatch.
     *
     * For example, for <pre>fun &lt;R&gt; foo(t: List&lt;R&gt;) {}</pre> in invocation <tt>foo(hashSet("s"))</tt>
     * there is type constructor mismatch: <tt>"HashSet&lt;String&gt; cannot be a subtype of List&lt;R&gt;"</tt>.
     */
    public fun hasTypeConstructorMismatch(): Boolean

    /**
     * Returns <tt>true</tt> if there is type constructor mismatch only in constraintPosition or
     * constraint system is successful without constraints from this position.
     */
    public fun hasOnlyErrorsFromPosition(constraintPosition: ConstraintPosition): Boolean

    /**
     * Returns <tt>true</tt> if there is an error in constraining types. <p/>
     * Is used not to generate type inference error if there was one in argument types.
     */
    public fun hasErrorInConstrainingTypes(): Boolean

    /**
     * Returns <tt>true</tt> if a user type contains the type projection that cannot be captured.
     *
     * For example, for <pre>fun &lt;T&gt; foo(t: Array&lt;Array&lt;T&gt;&gt;) {}</pre>
     * in invocation <tt>foo(array)</tt> where array has type <tt>Array&lt;Array&lt;out Int&gt;&gt;</tt>.
     */
    public fun hasCannotCaptureTypesError(): Boolean
}
