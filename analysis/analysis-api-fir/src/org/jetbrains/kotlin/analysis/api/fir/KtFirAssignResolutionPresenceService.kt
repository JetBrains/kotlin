/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getFirResolveSession
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.analysis.providers.KtAssignResolutionPresenceService
import org.jetbrains.kotlin.fir.extensions.assignAltererExtensions
import org.jetbrains.kotlin.fir.extensions.extensionService

@Suppress("unused")
class KtFirAssignResolutionPresenceService : KtAssignResolutionPresenceService() {
    override fun assignResolutionIsOn(module: KtModule): Boolean {
        val firResolveSession = module.getFirResolveSession(module.project)
        return firResolveSession.useSiteFirSession.extensionService.assignAltererExtensions.isNotEmpty()
    }
}