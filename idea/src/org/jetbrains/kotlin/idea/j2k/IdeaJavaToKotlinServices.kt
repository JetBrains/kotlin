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

package org.jetbrains.kotlin.idea.j2k

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiVariable
import org.jetbrains.kotlin.idea.util.compat.dfaCheckNullability
import org.jetbrains.kotlin.idea.util.compat.dfaInferMethodNullability
import org.jetbrains.kotlin.j2k.*
import org.jetbrains.kotlin.j2k.ast.Nullability
import org.jetbrains.kotlin.idea.util.compat.Nullability as IntellijNullability

object IdeaJavaToKotlinServices : JavaToKotlinConverterServices {
    override val referenceSearcher: ReferenceSearcher
        get() = IdeaReferenceSearcher

    override val superMethodsSearcher: SuperMethodsSearcher
        get() = IdeaSuperMethodSearcher

    override val resolverForConverter: ResolverForConverter
        get() = IdeaResolverForConverter

    override val docCommentConverter: DocCommentConverter
        get() = IdeaDocCommentConverter

    override val javaDataFlowAnalyzerFacade: JavaDataFlowAnalyzerFacade
        get() = IdeaJavaDataFlowAnalyzerFacade
}

object IdeaSuperMethodSearcher : SuperMethodsSearcher {
    override fun findDeepestSuperMethods(method: PsiMethod) = method.findDeepestSuperMethods().asList()
}

private object IdeaJavaDataFlowAnalyzerFacade : JavaDataFlowAnalyzerFacade {
    override fun variableNullability(variable: PsiVariable, context: PsiElement): Nullability =
        dfaCheckNullability(variable, context).toJ2KNullability()

    override fun methodNullability(method: PsiMethod): Nullability =
        dfaInferMethodNullability(method).toJ2KNullability()

    private fun IntellijNullability.toJ2KNullability() = when (this) {
        IntellijNullability.UNKNOWN -> Nullability.Default
        IntellijNullability.NOT_NULL -> Nullability.NotNull
        IntellijNullability.NULLABLE -> Nullability.Nullable
    }
}

object IdeaNewJavaToKotlinServices : NewJavaToKotlinServices {
    override val oldServices: JavaToKotlinConverterServices
        get() = IdeaJavaToKotlinServices

}