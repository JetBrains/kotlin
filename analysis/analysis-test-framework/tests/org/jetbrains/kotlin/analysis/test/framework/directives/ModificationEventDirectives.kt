/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.directives

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.analysis.providers.analysisMessageBus
import org.jetbrains.kotlin.analysis.providers.topics.KotlinModuleStateModificationKind
import org.jetbrains.kotlin.analysis.providers.topics.KotlinTopics
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.directives.model.singleValue
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.testFramework.runWriteAction

object ModificationEventDirectives : SimpleDirectivesContainer() {
    val MODIFICATION_EVENT by enumDirective<ModificationEventKind>(
        description = "The modification event to be raised by the test.",
    )
}

enum class ModificationEventKind {
    MODULE_STATE_MODIFICATION,
    MODULE_OUT_OF_BLOCK_MODIFICATION,
    GLOBAL_MODULE_STATE_MODIFICATION,
    GLOBAL_SOURCE_MODULE_STATE_MODIFICATION,
    GLOBAL_SOURCE_OUT_OF_BLOCK_MODIFICATION,
}

/**
 * Publishes a modification event as defined in [KotlinTopics][org.jetbrains.kotlin.analysis.providers.topics.KotlinTopics] based on the
 * [ModificationEventDirectives.MODIFICATION_EVENT] directive in a write action.
 *
 * The function expects exactly one `MODIFICATION_EVENT` directive to be present in the registered directives.
 *
 * @param module The module for which module-level modification events should be published, or `null` if only global modification events
 *  should be published. (Specifying a module-level modification event kind then raises an error.)
 */
fun TestModule.publishModificationEventByDirective(project: Project, module: KtModule?) {
    val modificationEventKind = directives.singleValue(ModificationEventDirectives.MODIFICATION_EVENT)

    publishModificationEventByKind(modificationEventKind, project, module)
}

private fun publishModificationEventByKind(modificationEventKind: ModificationEventKind, project: Project, module: KtModule?) {
    runWriteAction {
        when (modificationEventKind) {
            ModificationEventKind.MODULE_STATE_MODIFICATION -> {
                project.analysisMessageBus
                    .syncPublisher(KotlinTopics.MODULE_STATE_MODIFICATION)
                    .onModification(
                        module ?: errorModuleRequired(),
                        KotlinModuleStateModificationKind.UPDATE,
                    )
            }

            ModificationEventKind.MODULE_OUT_OF_BLOCK_MODIFICATION -> {
                project.analysisMessageBus
                    .syncPublisher(KotlinTopics.MODULE_OUT_OF_BLOCK_MODIFICATION)
                    .onModification(module ?: errorModuleRequired())
            }

            ModificationEventKind.GLOBAL_MODULE_STATE_MODIFICATION -> {
                project.analysisMessageBus.syncPublisher(KotlinTopics.GLOBAL_MODULE_STATE_MODIFICATION).onModification()
            }

            ModificationEventKind.GLOBAL_SOURCE_MODULE_STATE_MODIFICATION -> {
                project.analysisMessageBus.syncPublisher(KotlinTopics.GLOBAL_SOURCE_MODULE_STATE_MODIFICATION).onModification()
            }

            ModificationEventKind.GLOBAL_SOURCE_OUT_OF_BLOCK_MODIFICATION -> {
                project.analysisMessageBus.syncPublisher(KotlinTopics.GLOBAL_SOURCE_OUT_OF_BLOCK_MODIFICATION).onModification()
            }
        }
    }
}

private fun errorModuleRequired(): Nothing = error("Cannot publish a module-level modification event without a module.")
