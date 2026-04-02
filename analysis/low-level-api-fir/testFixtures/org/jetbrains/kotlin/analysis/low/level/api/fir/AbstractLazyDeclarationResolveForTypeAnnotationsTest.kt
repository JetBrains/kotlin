/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir

import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirOutOfContentRootTestConfigurator
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.LLSourceLikeTestConfigurator

abstract class AbstractLazyDeclarationResolveForTypeAnnotationsTest : AbstractFirLazyDeclarationResolveTest() {
    override val outputExtension: String get() = ".lazyResolve.txt"
}

abstract class AbstractSourceLikeLazyDeclarationResolveForTypeAnnotationsTest : AbstractLazyDeclarationResolveForTypeAnnotationsTest() {
    override val configurator = LLSourceLikeTestConfigurator()
}

abstract class AbstractOutOfContentRootLazyDeclarationResolveForTypeAnnotationsTest :
    AbstractLazyDeclarationResolveForTypeAnnotationsTest() {
    override val configurator get() = AnalysisApiFirOutOfContentRootTestConfigurator
}
