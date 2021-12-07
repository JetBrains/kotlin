/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jvm.compiler

import org.jetbrains.kotlin.test.util.KtTestUtil

class Java17ModulesIntegrationTest : JavaModulesIntegrationTest(17, KtTestUtil.getJdk17Home())
