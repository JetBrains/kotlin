/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jvm.compiler

import org.jetbrains.kotlin.test.util.KtTestUtil

class Java11ModulesIntegrationTest : JavaModulesIntegrationTest(11, KtTestUtil.getJdk11Home())
