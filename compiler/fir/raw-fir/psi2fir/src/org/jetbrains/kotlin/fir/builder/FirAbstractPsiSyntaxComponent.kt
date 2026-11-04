/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.builder

import org.jetbrains.kotlin.fir.FirSyntaxComponent

/**
 * Base class for the [FirSyntaxComponent]s that work on top of the IntelliJ platform trees (PSI and LightTree).
 */
abstract class FirAbstractPsiSyntaxComponent : FirSyntaxComponent()
