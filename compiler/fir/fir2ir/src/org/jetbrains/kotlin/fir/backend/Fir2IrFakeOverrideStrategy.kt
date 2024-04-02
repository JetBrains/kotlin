/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import org.jetbrains.kotlin.ir.overrides.FakeOverrideBuilderStrategy

internal class Fir2IrFakeOverrideStrategy(
    friendModules: Map<String, List<String>>,
    override val isGenericClashFromSameSupertypeAllowed: Boolean,
    override val isOverrideOfPublishedApiFromOtherModuleDisallowed: Boolean,
) : FakeOverrideBuilderStrategy.BindToPrivateSymbols(friendModules)
