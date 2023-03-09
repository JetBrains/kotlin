/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services

import org.jetbrains.kotlin.fir.signaturer.FirMangler
import org.jetbrains.kotlin.ir.util.KotlinMangler
import org.jetbrains.kotlin.test.model.TestModule

class ManglerProvider(@Suppress("UNUSED_PARAMETER") testServices: TestServices) : TestService {
    data class Manglers(
        val descriptorMangler: KotlinMangler.DescriptorMangler,
        val irMangler: KotlinMangler.IrMangler,
        val firMangler: FirMangler?,
    )

    private val manglersForModules = hashMapOf<TestModule, Manglers>()

    fun getManglersForModule(module: TestModule): Manglers =
        manglersForModules[module] ?: error("No manglers found for module $module")

    fun setManglersForModule(module: TestModule, manglers: Manglers) {
        if (module in manglersForModules) {
            error("Manglers for module $module already exist")
        }
        manglersForModules[module] = manglers
    }
}

val TestServices.manglerProvider: ManglerProvider by TestServices.testServiceAccessor()
