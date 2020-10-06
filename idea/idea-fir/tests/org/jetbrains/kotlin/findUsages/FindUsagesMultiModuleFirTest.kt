/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.findUsages

import org.jetbrains.kotlin.doTestWithFIRFlags

class FindUsagesMultiModuleFirTest : FindUsagesMultiModuleTest() {
    override val isFirPlugin: Boolean = true

    override fun doFindUsagesTest() = doTestWithFIRFlags(mainFile.text) {
        super.doFindUsagesTest()
    }
}