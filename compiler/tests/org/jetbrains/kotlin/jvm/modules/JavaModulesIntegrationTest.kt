/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jvm.modules

import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.test.util.KtTestUtil

class Java11ModulesIntegrationTest : AbstractJavaModulesIntegrationTest(11, KtTestUtil.getJdk11Home(), LanguageVersion.KOTLIN_1_9)

class Java17ModulesIntegrationTest : AbstractJavaModulesIntegrationTest(17, KtTestUtil.getJdk17Home(), LanguageVersion.KOTLIN_1_9)

class FirJava11ModulesIntegrationTest : AbstractFirJavaModulesIntegrationTest(11, KtTestUtil.getJdk11Home())

class FirJava17ModulesIntegrationTest : AbstractFirJavaModulesIntegrationTest(17, KtTestUtil.getJdk17Home())
