/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.grouping

import org.jetbrains.kotlin.test.services.BatchingPackageInserter.Companion.computePackage
import org.jetbrains.kotlin.test.services.KotlinTestInfo

/**
 * The name of the synthetic per-test launcher that a grouped batch's generated launcher source declares for [testInfo],
 * and the id the batch's per-test outcome is attributed by on the JVM side.
 *
 * It has to be derived from the test alone, since the generating side (the stage-2 grouping facade) and the consuming
 * side (the grouped batch runner) compute it independently. The hash is taken over the per-test additional package
 * (see [computePackage]), which already uniquely identifies the test, so that the result stays short enough for
 * filesystem paths and for a minified JS export name.
 *
 * Shared between the targets that group tests — K/Wasm keys its `ProxyLauncher_<hash>` classes by it, K/JS its exported
 * `ProxyLauncher_<hash>()` functions — so that a change to the scheme cannot desynchronize one target's generator from
 * its runner.
 */
fun computeProxyLauncherClassName(testInfo: KotlinTestInfo): String =
    "ProxyLauncher_${computePackage(testInfo).hashCode().toUInt().toString(36)}"

