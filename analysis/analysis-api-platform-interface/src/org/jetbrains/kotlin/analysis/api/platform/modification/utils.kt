/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.analysis.api.platform.modification

import com.intellij.openapi.project.Project
import com.intellij.util.concurrency.ThreadingAssertions
import org.jetbrains.kotlin.analysis.api.platform.analysisMessageBus
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule

/**
 * Publishes a [KotlinModificationEvent] to the project's [analysisMessageBus]. Must be called in a write action.
 */
public fun Project.publishModificationEvent(event: KotlinModificationEvent) {
    ThreadingAssertions.assertWriteAccess()

    analysisMessageBus.syncPublisher(KotlinModificationEvent.TOPIC).onModification(event)
}

/**
 * Publishes a [KotlinModuleStateModificationEvent] for this [KaModule]. Must be called in a write action.
 */
public fun KaModule.publishModuleStateModificationEvent(modificationKind: KotlinModuleStateModificationKind) {
    project.publishModificationEvent(KotlinModuleStateModificationEvent(this, modificationKind))
}

/**
 * Publishes a [KotlinModuleOutOfBlockModificationEvent] for this [KaModule]. Must be called in a write action.
 */
public fun KaModule.publishModuleOutOfBlockModificationEvent() {
    project.publishModificationEvent(KotlinModuleOutOfBlockModificationEvent(this))
}

/**
 * Publishes a [KotlinGlobalModuleStateModificationEvent]. Must be called in a write action.
 */
public fun Project.publishGlobalModuleStateModificationEvent() {
    publishModificationEvent(KotlinGlobalModuleStateModificationEvent)
}

/**
 * Publishes a [KotlinGlobalSourceModuleStateModificationEvent]. Must be called in a write action.
 */
public fun Project.publishGlobalSourceModuleStateModificationEvent() {
    publishModificationEvent(KotlinGlobalSourceModuleStateModificationEvent)
}

/**
 * Publishes a [KotlinGlobalScriptModuleStateModificationEvent]. Must be called in a write action.
 */
public fun Project.publishGlobalScriptModuleStateModificationEvent() {
    publishModificationEvent(KotlinGlobalScriptModuleStateModificationEvent)
}

/**
 * Publishes a [KotlinGlobalSourceOutOfBlockModificationEvent]. Must be called in a write action.
 */
public fun Project.publishGlobalSourceOutOfBlockModificationEvent() {
    publishModificationEvent(KotlinGlobalSourceOutOfBlockModificationEvent)
}

/**
 * Publishes a [KotlinCodeFragmentContextModificationEvent] for this [KaModule]. Must be called in a write action.
 */
public fun KaModule.publishCodeFragmentContextModificationEvent() {
    project.publishModificationEvent(KotlinCodeFragmentContextModificationEvent(this))
}
