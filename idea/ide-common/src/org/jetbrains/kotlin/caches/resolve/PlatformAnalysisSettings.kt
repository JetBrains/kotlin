/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.caches.resolve

/**
 * Regulates which sources should be analyzed together.
 *
 * There are exactly two descendants, which are in strong one-to-one correspondence with [ResolutionModeComponent.Mode] (meaning
 * that after checking value of ResolutionMode, it's safe to downcast settings instance to the respective type):
 * - [PlatformAnalysisSettingsImpl] should be used iff we're working under [Mode.SEPARATE], and will create separate
 *   facade for each platforms, sdk, builtIns settings and other stuff.
 *   This is the old and stable mode, which should be used by default.
 *
 * - [CompositeAnalysisSettings] should be used iff we're working under [Mode.COMPOSITE], and will analyze all sources
 *   together, in one facade.
 *   This mode is new and experimental, and works only together with TypeRefinement facilities in the compiler's frontend.
 *   This mode is currently enabled only for HMPP projects
 */
interface PlatformAnalysisSettings