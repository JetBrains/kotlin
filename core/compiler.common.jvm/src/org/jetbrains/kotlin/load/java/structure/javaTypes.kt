/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.load.java.structure

import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.name.ClassId

interface JavaType : ListBasedJavaAnnotationOwner {
    /**
     * Whether [filterTypeUseAnnotations] does anything beyond returning [annotations]. Lets the
     * caller skip allocating the callback closure on impls that pre-filter at structure-build
     * time (PSI/binary). java-direct overrides this to `true` because its annotations are not
     * pre-filtered.
     */
    val needsTypeUseAnnotationFiltering: Boolean get() = false

    /**
     * Filters annotations to only include TYPE_USE annotations.
     *
     * This is used when converting Java types to FIR - only annotations with
     * `@Target(ElementType.TYPE_USE)` should appear on types.
     *
     * The default implementation returns all annotations unchanged, assuming that
     * the implementation has already filtered them (as javac-wrapper does at the
     * Java structure level).
     *
     * Implementations that don't pre-filter (like java-direct) should override this
     * to use the callback for filtering, and also override [needsTypeUseAnnotationFiltering].
     *
     * @param isTypeUseAnnotation callback that checks if a given annotation class has TYPE_USE target.
     *        The callback receives the fully qualified annotation class name and returns true if it's TYPE_USE.
     * @return collection of annotations that are TYPE_USE annotations
     */
    fun filterTypeUseAnnotations(isTypeUseAnnotation: (String) -> Boolean): Collection<JavaAnnotation> {
        // Default: return annotations as-is (javac-wrapper already filters at Java structure level)
        return annotations
    }
}

interface JavaArrayType : JavaType {
    val componentType: JavaType
}

interface JavaClassifierType : JavaType {
    val classifier: JavaClassifier?
    val typeArguments: List<JavaType?>

    val isRaw: Boolean

    val classifierQualifiedName: String
    val presentableText: String

    val isResolved: Boolean
        get() = true

    /**
     * Resolved [ClassId] hint, populated by `java-direct`'s injected resolver for cross-file
     * references that [classifier] cannot answer.
     *
     * **Post-`java-direct` Step 4.5a contract** (per
     * `compiler/java-direct/implDocs/FIRSESSION_INJECTION_PROPOSAL_2026_05_05.md` §3): when
     * non-null, this is the FIR-side resolved [ClassId] for this type reference (the answer
     * the deleted `resolve(tryResolve, getSupertypeClassIds)` callback API used to compute).
     * `JavaTypeConversion.resolveTypeName` reads this as a primary source of truth before
     * falling back to `findClassIdByFqNameString` / `ClassId.topLevel`.
     *
     * Pre-`java-direct` impls (PSI, binary) return `null` and let FIR's pre-`java-direct`
     * fallback do the FQN probing.
     */
    val resolvedClassId: ClassId?
        get() = null

    /**
     * Hint for FIR type conversion that this classifier type should produce a trivially flexible
     * ConeFlexibleType (isTrivial=true), even when the classifier is null (cross-file reference).
     *
     * When true, [org.jetbrains.kotlin.fir.java.JavaTypeConversion] will use
     * [ConeRigidType.toTrivialFlexibleType] instead of constructing an explicit upper bound,
     * producing compact `T!` FIR dump output instead of `ft<T, T?>`.
     *
     * The default returns false. java-direct overrides this for user-defined Java source classes
     * that are known to be trivially flexible (matching PSI behavior).
     */
    val isTriviallyFlexibleHint: Boolean get() = false

    /**
     * ClassIds of classes in the containing scope chain, from innermost to outermost.
     *
     * When this type reference appears inside a class declaration (e.g., as a supertype),
     * this returns the ClassId of that class plus its outer classes. This allows FIR
     * to find outer class type arguments for inherited inner class types by walking
     * the containing class's supertype chain.
     *
     * For example, for `NestedInSuperClass` in `class J1.NestedSubClass extends NestedInSuperClass`,
     * this returns `[J1.NestedSubClass, J1]`.
     *
     * The default returns an empty list (PSI types resolve outer type args via PsiSubstitutor).
     */
    val containingClassIds: List<ClassId> get() = emptyList()
}

interface JavaPrimitiveType : JavaType {
    /** `null` means the `void` type. */
    val type: PrimitiveType?
}

interface JavaWildcardType : JavaType {
    val bound: JavaType?
    val isExtends: Boolean
}

fun JavaType?.isSuperWildcard(): Boolean = (this as? JavaWildcardType)?.let { it.bound != null && !it.isExtends } ?: false
