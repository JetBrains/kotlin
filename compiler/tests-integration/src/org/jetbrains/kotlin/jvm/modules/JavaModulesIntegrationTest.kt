/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jvm.modules

import org.jetbrains.kotlin.config.LanguageVersion

class JavaModulesIntegrationTest : AbstractJavaModulesIntegrationTest(LanguageVersion.KOTLIN_1_9)

class FirJavaModulesIntegrationTest : AbstractJavaModulesIntegrationTest(LanguageVersion.LATEST_STABLE)
