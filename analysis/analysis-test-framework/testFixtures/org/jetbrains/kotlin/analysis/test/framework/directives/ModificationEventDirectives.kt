/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.directives

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.api.platform.modification.*
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibraryFallbackDependenciesModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibraryModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibrarySourceModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModuleStructure
import org.jetbrains.kotlin.test.directives.model.DirectiveApplicability
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.directives.model.singleOrZeroValue
import org.jetbrains.kotlin.test.testFramework.runWriteAction

/**
 * Modification event directives allow publishing modification events in a specific module or globally.
 *
 * These directives are available by default in Analysis API tests and do not need to be registered separately. They are not applied
 * automatically, however, and tests interested in publishing modification events must call one of the following publishing functions for
 * the appropriate [KtTestModule]: [publishModificationEventByDirective] or [publishWildcardModificationEventByDirectiveIfPresent].
 */
object ModificationEventDirectives : SimpleDirectivesContainer() {
    val MODIFICATION_EVENT by enumDirective<KotlinModificationEventKind>(
        description = "The modification event to be raised for the module or globally.",
    )

    val WILDCARD_MODIFICATION_EVENT by directive(
        description = """
            Specifies that a modification event should be raised for the module or globally, but the test has to specify the modification 
            event kind itself. This allows generating multiple tests which raise different modification events over the same test data.
        """.trimIndent(),
    )

    val MODIFICATION_EVENT_TARGET by enumDirective<ModificationEventDirectiveTarget>(
        description = """
            Specifies the module for which the modification event should be published. Only has an effect for module-level modification 
            events.
        """.trimIndent(),
        applicability = DirectiveApplicability.Module,
    )
}

/**
 * Specifies the module for which the modification event should be published. Only has an effect for module-level modification events.
 *
 * @see ModificationEventDirectives.MODIFICATION_EVENT_TARGET
 */
enum class ModificationEventDirectiveTarget {
    /**
     * The target of the modification event is the test module itself.
     *
     * This is equivalent to not specifying the [ModificationEventDirectives.MODIFICATION_EVENT_TARGET] directive at all.
     */
    SELF,

    /**
     * The target of the modification event is the fallback dependencies module of the test module. This target is only applicable to a
     * library (source) module with fallback dependencies.
     *
     * @see org.jetbrains.kotlin.analysis.api.projectStructure.KaLibraryFallbackDependenciesModule
     */
    FALLBACK_DEPENDENCIES,
}

/**
 * Publishes a [KotlinModificationEvent] based on the [ModificationEventDirectives.MODIFICATION_EVENT] directive present in the test module
 * in a write action.
 *
 * Module-level modification events will be published for the [KtTestModule]'s [KaModule], or another module depending on
 * [ModificationEventDirectives.MODIFICATION_EVENT_TARGET].
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
    publishModificationEvent(modificationEventKind)
}

/**
 * If the given test module contains a [ModificationEventDirectives.WILDCARD_MODIFICATION_EVENT] directive, publishes a
 * [KotlinModificationEvent] based on the given [modificationEventKind] in a write action.
 *
 * Module-level modification events will be published for the [KtTestModule]'s [KaModule], or another module depending on
 * [ModificationEventDirectives.MODIFICATION_EVENT_TARGET].
 */
fun KtTestModule.publishWildcardModificationEventByDirectiveIfPresent(modificationEventKind: KotlinModificationEventKind) {
    if (ModificationEventDirectives.WILDCARD_MODIFICATION_EVENT in testModule.directives) {
        publishModificationEvent(modificationEventKind)
    }
}

/**
 * For each test module that contains a [ModificationEventDirectives.WILDCARD_MODIFICATION_EVENT] directive, publishes a
 * [KotlinModificationEvent] based on the given [modificationEventKind] in a write action.
 *
 * Global-level modification events will only be published *once*, regardless of how many `WILDCARD_MODIFICATION_EVENT` directives the test
 * modules contain, as long as at least one test module contains it (to support test cases which don't want to publish any modification
 * events).
 */
fun KtTestModuleStructure.publishWildcardModificationEventsByDirective(modificationEventKind: KotlinModificationEventKind) {
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

private fun KtTestModule.publishModificationEvent(modificationEventKind: KotlinModificationEventKind) {
    val targetModule = when (modificationEventDirectiveTarget) {
        ModificationEventDirectiveTarget.SELF -> ktModule
        ModificationEventDirectiveTarget.FALLBACK_DEPENDENCIES -> ktModule.getFallbackDependenciesModule()
    }
    publishModificationEventByKind(modificationEventKind, targetModule.project, targetModule)
}

private val KtTestModule.modificationEventDirectiveTarget: ModificationEventDirectiveTarget
    get() = testModule.directives.singleOrZeroValue(ModificationEventDirectives.MODIFICATION_EVENT_TARGET)
        ?: ModificationEventDirectiveTarget.SELF

private fun KaModule.getFallbackDependenciesModule(): KaModule {
    require(this is KaLibraryModule || this is KaLibrarySourceModule) {
        "The MODIFICATION_EVENT_TARGET=${ModificationEventDirectiveTarget.FALLBACK_DEPENDENCIES.name} directive can only be applied to a" +
                " library module or library source module, but the module '${this}' is of type `${this::class.simpleName}`."
    }

    return directRegularDependencies.singleOrNull { it is KaLibraryFallbackDependenciesModule }
        ?: error("Expected a single fallback dependencies module for the module '${this}'.")
}

private fun publishGlobalModificationEvent(modificationEventKind: KotlinModificationEventKind, project: Project) {
    require(modificationEventKind.isGlobalLevel)

    publishModificationEventByKind(modificationEventKind, project, ktModule = null)
}

private fun publishModificationEventByKind(modificationEventKind: KotlinModificationEventKind, project: Project, ktModule: KaModule?) {
    val modificationEvent = when (modificationEventKind) {
        KotlinModificationEventKind.MODULE_STATE_MODIFICATION ->
            KotlinModuleStateModificationEvent(
                ktModule ?: errorModuleRequired(),
                KotlinModuleStateModificationKind.UPDATE,
            )

        KotlinModificationEventKind.MODULE_OUT_OF_BLOCK_MODIFICATION ->
            KotlinModuleOutOfBlockModificationEvent(ktModule ?: errorModuleRequired())

        KotlinModificationEventKind.GLOBAL_MODULE_STATE_MODIFICATION ->
            KotlinGlobalModuleStateModificationEvent

        KotlinModificationEventKind.GLOBAL_SOURCE_MODULE_STATE_MODIFICATION ->
            KotlinGlobalSourceModuleStateModificationEvent

        KotlinModificationEventKind.GLOBAL_SCRIPT_MODULE_STATE_MODIFICATION ->
            KotlinGlobalScriptModuleStateModificationEvent

        KotlinModificationEventKind.GLOBAL_SOURCE_OUT_OF_BLOCK_MODIFICATION ->
            KotlinGlobalSourceOutOfBlockModificationEvent

        KotlinModificationEventKind.CODE_FRAGMENT_CONTEXT_MODIFICATION ->
            KotlinCodeFragmentContextModificationEvent(ktModule ?: errorModuleRequired())
    }

    runWriteAction {
        project.publishModificationEvent(modificationEvent)
    }
}

private fun errorModuleRequired(): Nothing = error("Cannot publish a module-level modification event without a module.")
