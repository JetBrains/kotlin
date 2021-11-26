/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.test.symbols

import org.jetbrains.kotlin.analysis.api.descriptors.test.KtFe10FrontendApiTestConfiguratorService
import org.jetbrains.kotlin.analysis.api.impl.base.test.symbols.AbstractSymbolByFqNameTest

abstract class AbstractKtFe10SymbolByFqNameTest : AbstractSymbolByFqNameTest(KtFe10FrontendApiTestConfiguratorService)