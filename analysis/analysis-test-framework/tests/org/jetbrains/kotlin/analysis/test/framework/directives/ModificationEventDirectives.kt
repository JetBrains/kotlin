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
import org.jetbrains.kotlin.analysis.test.framework.project.structure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.project.structure.KtTestModuleStructure
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.testFramework.runWriteAction

/**
 * Modification event directives allow publishing modification events in a specific module or globally.
 *
 * These directives are available by default in Analysis API tests and do not need to be registered separately. They are not applied
 * automatically, however, and tests interested in publishing modification events must call one of the following publishing functions for
 * the appropriate [KtTestModule]: [publishModificationEventByDirective] or [publishWildcardModificationEventByDirectiveIfPresent].
 */
object ModificationEventDirectives : SimpleDirectivesContainer() {
    val MODIFICATION_EVENT by enumDirective<ModificationEventKind>(
        description = "The modification event to be raised for the module or globally.",
    )

    val WILDCARD_MODIFICATION_EVENT by directive(
        description = """
            Specifies that a modification event should be raised for the module or globally, but the test has to specify the modification 
            event kind itself. This allows generating multiple tests which raise different modification events over the same test data.
        """.trimIndent()
    )
}

enum class ModificationEventKind {
    MODULE_STATE_MODIFICATION,
    MODULE_OUT_OF_BLOCK_MODIFICATION,
    GLOBAL_MODULE_STATE_MODIFICATION,
    GLOBAL_SOURCE_MODULE_STATE_MODIFICATION,
    GLOBAL_SOURCE_OUT_OF_BLOCK_MODIFICATION,
    CODE_FRAGMENT_CONTEXT_MODIFICATION,
}

val ModificationEventKind.isModuleLevel: Boolean
    get() = this == ModificationEventKind.MODULE_STATE_MODIFICATION ||
            this == ModificationEventKind.MODULE_OUT_OF_BLOCK_MODIFICATION ||
            this == ModificationEventKind.CODE_FRAGMENT_CONTEXT_MODIFICATION

val ModificationEventKind.isGlobalLevel: Boolean
    get() = !isModuleLevel

/**
 * Publishes a modification event as defined in [KotlinTopics][org.jetbrains.kotlin.analysis.providers.topics.KotlinTopics] based on the
 * [ModificationEventDirectives.MODIFICATION_EVENT] directive present in the test module in a write action.
 *
 * Module-level modification events will be published for the [KtTestModule]'s [KtModule].
 *
 * The function expects exactly one `MODIFICATION_EVENT` directive to be present, unless [isOptional] is `true`.
 */
fun KtTestModule.publishModificationEventByDirective(isOptional: Boolean = false) {
    val modificationEventKinds = testModule.directives[ModificationEventDirectives.MODIFICATION_EVENT]
    val modificationEventKind = when (modificationEventKinds.size) {
        0 -> {
            if (isOptional) return
            error("Expected a `${ModificationEventDirectives.MODIFICATION_EVENT.name}` to be present in the test module `$this`.")
        }
        1 -> modificationEventKinds.single()
        else -> error("The test module `$this` must not specify multiple modification events.")
    }
    publishModificationEvent(modificationEventKind, ktModule)
}

/**
 * If the given test module contains a [ModificationEventDirectives.WILDCARD_MODIFICATION_EVENT] directive, publishes a modification event
 * as defined in [KotlinTopics][org.jetbrains.kotlin.analysis.providers.topics.KotlinTopics] based on the given [modificationEventKind] in
 * a write action.
 *
 * Module-level modification events will be published for the [KtTestModule]'s [KtModule].
 */
fun KtTestModule.publishWildcardModificationEventByDirectiveIfPresent(modificationEventKind: ModificationEventKind) {
    if (ModificationEventDirectives.WILDCARD_MODIFICATION_EVENT !in testModule.directives) {
        return
    }
    publishModificationEvent(modificationEventKind, ktModule)
}

/**
 * For each test module that contains a [ModificationEventDirectives.WILDCARD_MODIFICATION_EVENT] directive, publishes a modification event
 * as defined in [KotlinTopics][org.jetbrains.kotlin.analysis.providers.topics.KotlinTopics] based on the given [modificationEventKind] in
 * a write action.
 *
 * Global-level modification events will only be published *once*, regardless of how many `WILDCARD_MODIFICATION_EVENT` directives the test
 * modules contain, as long as at least one test module contains it (to support test cases which don't want to publish any modification
 * events).
 */
fun KtTestModuleStructure.publishWildcardModificationEventsByDirective(modificationEventKind: ModificationEventKind) {
    if (modificationEventKind.isModuleLevel) {
        mainModules.forEach { ktTestModule ->
            ktTestModule.publishWildcardModificationEventByDirectiveIfPresent(modificationEventKind)
        }
    } else {
        if (!testModuleStructure.allDirectives.contains(ModificationEventDirectives.WILDCARD_MODIFICATION_EVENT)) {
            return
        }
        publishGlobalModificationEvent(modificationEventKind, project)
    }
}

fun publishModificationEvent(modificationEventKind: ModificationEventKind, ktModule: KtModule) {
    publishModificationEventByKind(modificationEventKind, ktModule.project, ktModule)
}

fun publishGlobalModificationEvent(modificationEventKind: ModificationEventKind, project: Project) {
    require(modificationEventKind.isGlobalLevel)

    publishModificationEventByKind(modificationEventKind, project, ktModule = null)
}

private fun publishModificationEventByKind(modificationEventKind: ModificationEventKind, project: Project, ktModule: KtModule?) {
    runWriteAction {
        when (modificationEventKind) {
            ModificationEventKind.MODULE_STATE_MODIFICATION -> {
                project.analysisMessageBus
                    .syncPublisher(KotlinTopics.MODULE_STATE_MODIFICATION)
                    .onModification(
                        ktModule ?: errorModuleRequired(),
                        KotlinModuleStateModificationKind.UPDATE,
                    )
            }

            ModificationEventKind.MODULE_OUT_OF_BLOCK_MODIFICATION -> {
                project.analysisMessageBus
                    .syncPublisher(KotlinTopics.MODULE_OUT_OF_BLOCK_MODIFICATION)
                    .onModification(ktModule ?: errorModuleRequired())
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

            ModificationEventKind.CODE_FRAGMENT_CONTEXT_MODIFICATION -> {
                project.analysisMessageBus
                    .syncPublisher(KotlinTopics.CODE_FRAGMENT_CONTEXT_MODIFICATION)
                    .onModification(ktModule ?: errorModuleRequired())
            }
        }
    }
}

private fun errorModuleRequired(): Nothing = error("Cannot publish a module-level modification event without a module.")
