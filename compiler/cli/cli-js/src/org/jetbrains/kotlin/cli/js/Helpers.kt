/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.js

import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.psi.KtFile

fun checkKotlinPackageUsageForPsi(configuration: CompilerConfiguration, files: Collection<KtFile>) =
    org.jetbrains.kotlin.cli.common.checkKotlinPackageUsageForPsi(configuration, files)
