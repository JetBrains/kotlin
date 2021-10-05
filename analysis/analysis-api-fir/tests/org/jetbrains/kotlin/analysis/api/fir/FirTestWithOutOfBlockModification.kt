/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir

import com.intellij.openapi.components.ServiceManager
import org.jetbrains.kotlin.analysis.providers.KotlinModificationTrackerFactory
import org.jetbrains.kotlin.psi.KtFile

object FirTestWithOutOfBlockModification {
    fun doOutOfBlockModification(ktFile: KtFile) {
        ServiceManager.getService(ktFile.project, KotlinModificationTrackerFactory::class.java)
            .incrementModificationsCount()
    }
}