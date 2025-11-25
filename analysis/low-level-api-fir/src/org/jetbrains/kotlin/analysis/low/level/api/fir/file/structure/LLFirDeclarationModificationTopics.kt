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
 * @see org.jetbrains.kotlin.analysis.api.platform.modification.KotlinModificationEvent
 * @see LLFirDeclarationModificationService
 * */
internal object LLFirDeclarationModificationTopics {
    /**
     * @see ChangeType.InBlock
     */
    val IN_BLOCK_MODIFICATION: Topic<LLFirInBlockModificationListener> = Topic(
        /* listenerClass = */ LLFirInBlockModificationListener::class.java,
        /* broadcastDirection = */ Topic.BroadcastDirection.TO_CHILDREN,
        /* immediateDelivery = */ true,
    )
}

/**
 * @see LLFirDeclarationModificationTopics.IN_BLOCK_MODIFICATION
 * @see ChangeType.InBlock
 */
@KaImplementationDetail
interface LLFirInBlockModificationListener {
    /**
     * @param element the element where the in-block modification happened
     * @param module the module where the modification happened
     */
    fun afterModification(element: KtElement, module: KaModule)
}
