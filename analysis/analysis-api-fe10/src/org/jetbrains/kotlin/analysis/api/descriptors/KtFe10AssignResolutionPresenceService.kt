/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors

import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.analysis.providers.KtAssignResolutionPresenceService
import org.jetbrains.kotlin.extensions.internal.InternalNonStableExtensionPoints
import org.jetbrains.kotlin.resolve.extensions.AssignResolutionAltererExtension

@Suppress("unused")
class KtFe10AssignResolutionPresenceService : KtAssignResolutionPresenceService() {

    @OptIn(InternalNonStableExtensionPoints::class)
    override fun assignResolutionIsOn(module: KtModule): Boolean {
        return AssignResolutionAltererExtension.getInstances(module.project).isNotEmpty()
    }
}