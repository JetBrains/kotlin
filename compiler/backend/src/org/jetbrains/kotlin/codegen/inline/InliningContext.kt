/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.inline

import org.jetbrains.kotlin.codegen.ClassBuilder
import org.jetbrains.kotlin.codegen.state.GenerationState

class RootInliningContext(
    expressionMap: Map<Int, LambdaInfo>,
    state: GenerationState,
    nameGenerator: NameGenerator,
    val sourceCompilerForInline: SourceCompilerForInline,
    override val callSiteInfo: InlineCallSiteInfo,
    val inlineMethodReifier: ReifiedTypeInliner,
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
) {
    val continuationBuilders: MutableMap<String, ClassBuilder> = hashMapOf()
}

open class InliningContext(
    val parent: InliningContext?,
    val expressionMap: Map<Int, LambdaInfo>,
    val state: GenerationState,
    val nameGenerator: NameGenerator,
    val typeRemapper: TypeRemapper,
    val lambdaInfo: LambdaInfo?,
    val classRegeneration: Boolean
) {

    val isInliningLambda = lambdaInfo != null

    var generateAssertField = false

    private val internalNameToAnonymousObjectTransformationInfo = hashMapOf<String, AnonymousObjectTransformationInfo>()

    var isContinuation: Boolean = false

    val isRoot: Boolean = parent == null

    val root: RootInliningContext
        get() = if (isRoot) this as RootInliningContext else parent!!.root

    fun findAnonymousObjectTransformationInfo(internalName: String, searchInParent: Boolean = true): AnonymousObjectTransformationInfo? =
        internalNameToAnonymousObjectTransformationInfo[internalName]
            ?: if (searchInParent) parent?.findAnonymousObjectTransformationInfo(internalName, searchInParent) else null

    fun recordIfNotPresent(internalName: String, info: AnonymousObjectTransformationInfo) {
        internalNameToAnonymousObjectTransformationInfo.putIfAbsent(internalName, info)
    }

    fun subInlineLambda(lambdaInfo: LambdaInfo): InliningContext =
        subInline(
            nameGenerator.subGenerator("lambda"),
            //mark lambda inlined
            hashMapOf(lambdaInfo.lambdaClassType.internalName to null),
            lambdaInfo
        )

    fun subInlineWithClassRegeneration(
        generator: NameGenerator,
        newTypeMappings: MutableMap<String, String?>,
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
}
