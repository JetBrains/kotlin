/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.projectWizard.wizard

import org.jetbrains.kotlin.tools.projectWizard.core.Context
import org.jetbrains.kotlin.tools.projectWizard.core.context.SettingsWritingContext
import org.jetbrains.kotlin.tools.projectWizard.core.service.ServicesManager


class IdeContext(context: Context, servicesManager: ServicesManager, isUnitTestMode: Boolean) :
    SettingsWritingContext(
        context,
        servicesManager,
        isUnitTestMode
    ) {

    val eventManager
        get() = context.eventManager
}