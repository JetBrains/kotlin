/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jvm.modules

import org.jetbrains.kotlin.config.LanguageVersion
import java.io.File

abstract class AbstractFirJavaModulesIntegrationTest(jdkVersion: Int, jdkHome: File) : AbstractJavaModulesIntegrationTest(
    jdkVersion, jdkHome, LanguageVersion.LATEST_STABLE,
)
