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
import java.util.*

open class InliningContext(
        val parent: InliningContext?,
        private val expressionMap: Map<Int, LambdaInfo>,
        val state: GenerationState,
        val nameGenerator: NameGenerator,
        val typeRemapper: TypeRemapper,
        val reifiedTypeInliner: ReifiedTypeInliner,
        val isInliningLambda: Boolean,
        val classRegeneration: Boolean
) {
    val internalNameToAnonymousObjectTransformationInfo = hashMapOf<String, AnonymousObjectTransformationInfo>()

    var isContinuation: Boolean = false

    val isRoot: Boolean = parent == null

    val root: RootInliningContext
        get() = if (isRoot) this as RootInliningContext else parent!!.root

    fun subInline(generator: NameGenerator): InliningContext {
        return subInline(generator, emptyMap(), isInliningLambda)
    }

    fun subInlineLambda(lambdaInfo: LambdaInfo): InliningContext {
        val map = HashMap<String, String?>()
        map.put(lambdaInfo.lambdaClassType.internalName, null) //mark lambda inlined
        return subInline(nameGenerator.subGenerator("lambda"), map, true)
    }

    fun subInlineWithClassRegeneration(
            generator: NameGenerator,
            newTypeMappings: MutableMap<String, String>,
            callSiteInfo: InlineCallSiteInfo
    ): InliningContext {
        return RegeneratedClassContext(
                this, expressionMap, state, generator, TypeRemapper.createFrom(typeRemapper, newTypeMappings),
                reifiedTypeInliner, isInliningLambda, callSiteInfo
        )
    }

    private fun subInline(
            generator: NameGenerator, additionalTypeMappings: Map<String, String?>, isInliningLambda: Boolean
    ): InliningContext {
        //isInliningLambda && !this.isInliningLambda for root inline lambda
        return InliningContext(
                this, expressionMap, state, generator,
                TypeRemapper.createFrom(
                        typeRemapper,
                        additionalTypeMappings,
                        //root inline lambda
                        isInliningLambda && !this.isInliningLambda
                ),
                reifiedTypeInliner, isInliningLambda, classRegeneration
        )
    }

    open val callSiteInfo: InlineCallSiteInfo
        get() {
            return parent?.callSiteInfo ?: throw AssertionError("At least root context should return proper value")
        }

    fun findAnonymousObjectTransformationInfo(internalName: String): AnonymousObjectTransformationInfo? {
        if (root.internalNameToAnonymousObjectTransformationInfo.containsKey(internalName)) {
            return root.internalNameToAnonymousObjectTransformationInfo[internalName]
        }

        return null
    }
}
