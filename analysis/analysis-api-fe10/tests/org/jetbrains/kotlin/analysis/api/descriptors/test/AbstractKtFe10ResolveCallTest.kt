/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.test

import org.jetbrains.kotlin.analysis.api.impl.base.test.fir.AbstractResolveCallTest

abstract class AbstractKtFe10ResolveCallTest : AbstractResolveCallTest(KtFe10FrontendApiTestConfiguratorService)