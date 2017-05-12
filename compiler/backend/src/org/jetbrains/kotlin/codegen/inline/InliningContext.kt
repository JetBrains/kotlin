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

package org.jetbrains.kotlin.codegen.inline

import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.psi.KtElement

class RootInliningContext(
        expressionMap: Map<Int, LambdaInfo>,
        state: GenerationState,
        nameGenerator: NameGenerator,
        val callElement: KtElement,
        override val callSiteInfo: InlineCallSiteInfo,
        val reifiedTypeInliner: ReifiedTypeInliner,
        typeParameterMappings: TypeParameterMappings
) : InliningContext(
        null, expressionMap, state, nameGenerator, TypeRemapper.createRoot(typeParameterMappings), null, false
)

class RegeneratedClassContext(
        parent: InliningContext,
        expressionMap: Map<Int, LambdaInfo>,
        state: GenerationState,
        nameGenerator: NameGenerator,
        typeRemapper: TypeRemapper,
        lambdaInfo: LambdaInfo?,
        override val callSiteInfo: InlineCallSiteInfo
) : InliningContext(
        parent, expressionMap, state, nameGenerator, typeRemapper, lambdaInfo, true
)

open class InliningContext(
        val parent: InliningContext?,
        private val expressionMap: Map<Int, LambdaInfo>,
        val state: GenerationState,
        val nameGenerator: NameGenerator,
        val typeRemapper: TypeRemapper,
        val lambdaInfo: LambdaInfo?,
        val classRegeneration: Boolean
) {

    val isInliningLambda = lambdaInfo != null

    val internalNameToAnonymousObjectTransformationInfo = hashMapOf<String, AnonymousObjectTransformationInfo>()

    var isContinuation: Boolean = false

    val isRoot: Boolean = parent == null

    val root: RootInliningContext
        get() = if (isRoot) this as RootInliningContext else parent!!.root

    fun subInlineLambda(lambdaInfo: LambdaInfo): InliningContext =
            subInline(
                    nameGenerator.subGenerator("lambda"),
                    //mark lambda inlined
                    hashMapOf(lambdaInfo.lambdaClassType.internalName to null),
                    lambdaInfo
            )

    fun subInlineWithClassRegeneration(
            generator: NameGenerator,
            newTypeMappings: MutableMap<String, String>,
            callSiteInfo: InlineCallSiteInfo
    ): InliningContext = RegeneratedClassContext(
            this, expressionMap, state, generator, TypeRemapper.createFrom(typeRemapper, newTypeMappings),
            lambdaInfo, callSiteInfo
    )

    @JvmOverloads
    fun subInline(
            generator: NameGenerator,
            additionalTypeMappings: Map<String, String?> = emptyMap(),
            lambdaInfo: LambdaInfo? = this.lambdaInfo
    ): InliningContext {
        val isInliningLambda = lambdaInfo != null
        return InliningContext(
                this, expressionMap, state, generator,
                TypeRemapper.createFrom(
                        typeRemapper,
                        additionalTypeMappings,
                        //root inline lambda
                        isInliningLambda && !this.isInliningLambda
                ),
                lambdaInfo, classRegeneration
        )
    }

    open val callSiteInfo: InlineCallSiteInfo
        get() {
            return parent!!.callSiteInfo
        }

    fun findAnonymousObjectTransformationInfo(internalName: String): AnonymousObjectTransformationInfo? {
        return root.internalNameToAnonymousObjectTransformationInfo[internalName]
    }
}
