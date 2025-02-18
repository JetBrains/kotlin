/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure

import com.intellij.util.messages.Topic
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.psi.KtElement

/**
 * In-block modification is a source code modification that doesn't affect the state of other non-local declarations.
 * #### Example 1
 *
 * ```
 * val x: Int = 10<caret>
 * val z = x
 * ```
 *
 * If we change `10` to `"str"`, it would not change the type of `z`, so it is an **in-block-modification**.
 *
 * #### Example 2
 *
 * ```
 * val x = 10<caret>
 * val z = x
 * ```
 *
 * If we change the initializer of `x` to `"str"`, as in the first example,
 * the return type of `x` will become `String` instead of the initial `Int`.
 * This will change the return type of `z` as it does not have an explicit type.
 * So, it is an **out-of-block modification**.
 *
 * @see org.jetbrains.kotlin.analysis.api.platform.modification.KotlinModificationTopics
 * @see LLFirDeclarationModificationService
 * */
object LLFirDeclarationModificationTopics {
    val IN_BLOCK_MODIFICATION: Topic<LLFirInBlockModificationListener> = Topic(
        /* listenerClass = */ LLFirInBlockModificationListener::class.java,
        /* broadcastDirection = */ Topic.BroadcastDirection.TO_CHILDREN,
        /* immediateDelivery = */ true,
    )
}

/** @see LLFirDeclarationModificationTopics */
@KaImplementationDetail
interface LLFirInBlockModificationListener {
    /**
     * @param element an element where in-block modification happened
     * @param module a module where modification happened
     */
    fun afterModification(element: KtElement, module: KaModule)
}
