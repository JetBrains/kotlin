/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.kmm.wizard

import org.jetbrains.kotlin.tools.projectWizard.Versions
import org.jetbrains.kotlin.tools.projectWizard.core.service.KotlinVersionProviderService
import org.jetbrains.kotlin.tools.projectWizard.settings.version.Version

class KmmKotlinVersionProviderService : KotlinVersionProviderService {
    override fun getKotlinVersion(): Version = Versions.KOTLIN
}
