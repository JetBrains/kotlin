/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.inline

import org.jetbrains.kotlin.codegen.ClassBuilder
import org.jetbrains.kotlin.codegen.state.GenerationState

class RootInliningContext(
    state: GenerationState,
    nameGenerator: NameGenerator,
    val sourceCompilerForInline: SourceCompilerForInline,
    override val callSiteInfo: InlineCallSiteInfo,
    val inlineMethodReifier: ReifiedTypeInliner<*>,
    typeParameterMappings: TypeParameterMappings<*>
) : InliningContext(
    null, state, nameGenerator, TypeRemapper.createRoot(typeParameterMappings), null, false
)

class RegeneratedClassContext(
    parent: InliningContext,
    state: GenerationState,
    nameGenerator: NameGenerator,
    typeRemapper: TypeRemapper,
    lambdaInfo: LambdaInfo?,
    override val callSiteInfo: InlineCallSiteInfo,
    override val transformationInfo: TransformationInfo
) : InliningContext(
    parent, state, nameGenerator, typeRemapper, lambdaInfo, true
) {
    val continuationBuilders: MutableMap<String, ClassBuilder> = hashMapOf()
}

open class InliningContext(
    val parent: InliningContext?,
    val state: GenerationState,
    val nameGenerator: NameGenerator,
    val typeRemapper: TypeRemapper,
    val lambdaInfo: LambdaInfo?,
    val classRegeneration: Boolean
) {

    val isInliningLambda = lambdaInfo != null

    var generateAssertField = false

    open val transformationInfo: TransformationInfo?
        get() = null

    var isContinuation: Boolean = false

    val isRoot: Boolean = parent == null

    val root: RootInliningContext
        get() = if (isRoot) this as RootInliningContext else parent!!.root

    private val regeneratedAnonymousObjects = hashSetOf<String>()

    fun isRegeneratedAnonymousObject(internalName: String): Boolean =
        internalName in regeneratedAnonymousObjects || (parent != null && parent.isRegeneratedAnonymousObject(internalName))

    fun recordRegeneratedAnonymousObject(internalName: String) {
        regeneratedAnonymousObjects.add(internalName)
    }

    fun subInlineLambda(lambdaInfo: LambdaInfo): InliningContext =
        subInline(
            nameGenerator.subGenerator("lambda"),
            //mark lambda inlined
            hashMapOf(lambdaInfo.lambdaClassType.internalName to null),
            lambdaInfo,
            // TODO we also want this for the old backend (KT-28064), but this changes EnclosingMethod of objects
            //      in inline lambdas, so use a language version flag.
            if (state.isIrBackend)
                false // Do not regenerate objects in lambdas inlined into regenerated objects unless needed for some other reason.
            else
                classRegeneration
        )

    fun subInlineWithClassRegeneration(
        generator: NameGenerator,
        newTypeMappings: MutableMap<String, String?>,
        callSiteInfo: InlineCallSiteInfo,
        transformationInfo: TransformationInfo
    ): InliningContext = RegeneratedClassContext(
        this, state, generator, TypeRemapper.createFrom(typeRemapper, newTypeMappings),
        lambdaInfo, callSiteInfo, transformationInfo
    )

    @JvmOverloads
    fun subInline(
        generator: NameGenerator,
        additionalTypeMappings: Map<String, String?> = emptyMap(),
        lambdaInfo: LambdaInfo? = this.lambdaInfo,
        classRegeneration: Boolean = this.classRegeneration
    ): InliningContext {
        val isInliningLambda = lambdaInfo != null
        return InliningContext(
            this, state, generator,
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
