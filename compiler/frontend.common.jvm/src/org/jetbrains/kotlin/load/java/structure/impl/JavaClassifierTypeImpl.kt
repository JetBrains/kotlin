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

package org.jetbrains.kotlin.load.java.structure.impl

import com.intellij.psi.*
import org.jetbrains.kotlin.load.java.structure.JavaClassifierType
import org.jetbrains.kotlin.load.java.structure.JavaType
import org.jetbrains.kotlin.load.java.structure.impl.source.JavaElementTypeSource
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater

class JavaClassifierTypeImpl(
    psiClassTypeSource: JavaElementTypeSource<PsiClassType>,
) : JavaTypeImpl<PsiClassType>(psiClassTypeSource), JavaClassifierType {
    @Volatile
    private var resolutionResult: ResolutionResult? = null

    /**
     * Type arguments are cached because they can be heavily requested in some workloads. In Analysis API mode, [createTypeSource] creates a
     * smart type pointer, which can be an expensive operation.
     *
     * We use a simple volatile variable instead of a lazy value to reduce memory overhead. Some workloads can have hundreds of thousands of
     * [JavaClassifierTypeImpl] instances, meaning every byte counts. The computation is idempotent and publication does not need to be
     * synchronized.
     *
     * [JavaClassifierTypeImpl] instances do not exceed the lifetime of their containing cache (e.g., the FIR session). In particular, they
     * do not survive any modification of the associated PSI. The invalidation of [_typeArguments] is thus not necessary when the underlying
     * type arguments are modified, since a new instance of [JavaClassifierTypeImpl] would be created.
     */
    @Volatile
    private var _typeArguments: List<JavaType?>? = null

    override val classifier: JavaClassifierImpl<*>?
        get() = resolve().classifier

    val substitutor: PsiSubstitutor
        get() = resolve().substitutor

    override val classifierQualifiedName: String
        get() = psi.canonicalText.convertCanonicalNameToQName()

    override val presentableText: String
        get() = psi.presentableText

    override val isRaw: Boolean
        get() = resolve().isRaw

    /**
     * A list of type arguments for the type parameters of this class and its outer class(es).
     */
    override val typeArguments: List<JavaType?>
        get() {
            _typeArguments?.let { return it }

            val computed = computeTypeArguments()
            _typeArguments = computed
            return computed
        }

    private fun computeTypeArguments(): List<JavaType?> {
        val classifier = classifier as? JavaClassImpl ?: return emptyList()
        val parameters = getTypeParameters(classifier.psi)

        // Note: Returning `emptyList` is important so that we don't cache a new, empty array list instance.
        if (parameters.isEmpty()) return emptyList()

        val substitutor = substitutor

        val result = ArrayList<JavaType?>(parameters.size)
        for (typeParameter in parameters) {
            val substitutedType = substitutor.substitute(typeParameter)
            result.add(substitutedType?.let { create(createTypeSource(it)) })
        }
        return result
    }

    private class ResolutionResult(
        val classifier: JavaClassifierImpl<*>?,
        val substitutor: PsiSubstitutor,
        val isRaw: Boolean,
    ) {
        /**
         * Checks if the [ResolutionResult] is valid.
         *
         * The [PsiSubstitutor] which is contained inside [ResolutionResult] might become
         * invalidated as it contains [PsiType]s inside
         *
         * @return true if the substitutor is valid, false otherwise.
         */
        fun isValid(): Boolean {
            return substitutor.isValid
        }
    }

    /**
     * Resolves the current [JavaClassifierType]
     *
     * The code is thread safe and the logic is the following:
     * 1. Try to get a cached resolution result and return it if it's not invalidated
     * 2. Otherwise, resolve the current [JavaClassifierType], update the cache, and return the result.
     *
     * @returns The [ResolutionResult] to which the [JavaClassifierType] resolved.
     */
    private fun resolve(): ResolutionResult {
        while (true) {
            val snapshot = resolutionResult
            @Suppress("LiftReturnOrAssignment")
            when {
                snapshot != null && snapshot.isValid() -> {
                    return snapshot
                }

                else -> {
                    val computedResult = computeResolveResult()
                    if (!resolutionResultAtomicFieldUpdater.compareAndSet(this, snapshot, computedResult)) {
                        // some other thread already computed the value,
                        // we should get it on the next `while` loop iteration.
                        continue
                    }
                    return computedResult
                }
            }
        }
    }

    private fun computeResolveResult(): ResolutionResult {
        val result = psi.resolveGenerics()
        val psiClass = result.element
        val substitutor = result.substitutor
        return ResolutionResult(
            psiClass?.let { JavaClassifierImpl.create(it, sourceFactory) },
            substitutor,
            PsiClassType.isRaw(result)
        )
    }

    // Copy-pasted from PsiUtil.typeParametersIterable
    // The only change is using `Collections.addAll(result, typeParameters)` instead of reversing type parameters of `currentOwner`
    // Result differs in cases like:
    // class Outer<H1> {
    //   class Inner<H2, H3> {}
    // }
    //
    // PsiUtil.typeParametersIterable returns H3, H2, H1
    // But we would like to have H2, H3, H1 as such order is consistent with our type representation
    private fun getTypeParameters(owner: PsiClass): List<PsiTypeParameter> {
        var result: List<PsiTypeParameter>? = null

        var currentOwner: PsiTypeParameterListOwner? = owner
        while (currentOwner != null) {
            val typeParameters = currentOwner.typeParameters
            if (typeParameters.isNotEmpty()) {
                result = result?.let { it + typeParameters } ?: typeParameters.toList()
            }

            if (currentOwner.hasModifierProperty(PsiModifier.STATIC)) break
            currentOwner = currentOwner.containingClass
        }

        return result ?: emptyList()
    }

    companion object {
        @JvmStatic
        private val resolutionResultAtomicFieldUpdater = AtomicReferenceFieldUpdater.newUpdater(
            JavaClassifierTypeImpl::class.java,
            ResolutionResult::class.java,
            JavaClassifierTypeImpl::resolutionResult.name
        )
    }
}
