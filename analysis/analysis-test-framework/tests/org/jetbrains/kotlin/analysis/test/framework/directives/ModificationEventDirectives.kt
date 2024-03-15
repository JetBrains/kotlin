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
import org.jetbrains.kotlin.analysis.test.framework.project.structure.getKtModule
import org.jetbrains.kotlin.analysis.test.framework.services.environmentManager
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.testFramework.runWriteAction

/**
 * Modification event directives allow publishing modification events in a specific module or globally.
 *
 * These directives are available by default in Analysis API tests and do not need to be registered separately. They are not applied
 * automatically, however, and tests interested in publishing modification events must call one of the following publishing functions for
 * the appropriate [TestModule]: [publishModificationEventByDirective] or [publishWildcardModificationEventByDirectiveIfPresent].
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
}

val ModificationEventKind.isModuleLevel: Boolean
    get() = this == ModificationEventKind.MODULE_STATE_MODIFICATION || this == ModificationEventKind.MODULE_OUT_OF_BLOCK_MODIFICATION

val ModificationEventKind.isGlobalLevel: Boolean
    get() = !isModuleLevel

/**
 * Publishes a modification event as defined in [KotlinTopics][org.jetbrains.kotlin.analysis.providers.topics.KotlinTopics] based on the
 * [ModificationEventDirectives.MODIFICATION_EVENT] directive present in the test module in a write action.
 *
 * Module-level modification events will be published for the [TestModule]'s [KtModule].
 *
 * The function expects exactly one `MODIFICATION_EVENT` directive to be present, unless [isOptional] is `true`.
 */
fun TestModule.publishModificationEventByDirective(testServices: TestServices, isOptional: Boolean = false) {
    val modificationEventKinds = directives[ModificationEventDirectives.MODIFICATION_EVENT]
    val modificationEventKind = when (modificationEventKinds.size) {
        0 -> {
            if (isOptional) return
            error("Expected a `${ModificationEventDirectives.MODIFICATION_EVENT.name}` to be present in the test module `$this`.")
        }
        1 -> modificationEventKinds.single()
        else -> error("The test module `$this` must not specify multiple modification events.")
    }
    publishModificationEvent(modificationEventKind, getKtModule(testServices))
}

/**
 * If the given test module contains a [ModificationEventDirectives.WILDCARD_MODIFICATION_EVENT] directive, publishes a modification event
 * as defined in [KotlinTopics][org.jetbrains.kotlin.analysis.providers.topics.KotlinTopics] based on the given [modificationEventKind] in
 * a write action.
 *
 * Module-level modification events will be published for the [TestModule]'s [KtModule].
 */
fun TestModule.publishWildcardModificationEventByDirectiveIfPresent(
    modificationEventKind: ModificationEventKind,
    testServices: TestServices,
) {
    if (ModificationEventDirectives.WILDCARD_MODIFICATION_EVENT !in directives) {
        return
    }
    publishModificationEvent(modificationEventKind, getKtModule(testServices))
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
fun TestModuleStructure.publishWildcardModificationEventsByDirective(
    modificationEventKind: ModificationEventKind,
    testServices: TestServices,
) {
    if (modificationEventKind.isModuleLevel) {
        modules.forEach { testModule ->
            testModule.publishWildcardModificationEventByDirectiveIfPresent(modificationEventKind, testServices)
        }
    } else {
        if (!allDirectives.contains(ModificationEventDirectives.WILDCARD_MODIFICATION_EVENT)) {
            return
        }
        publishGlobalModificationEvent(modificationEventKind, testServices.environmentManager.getProject())
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
        }
    }
}

private fun errorModuleRequired(): Nothing = error("Cannot publish a module-level modification event without a module.")
