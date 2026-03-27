/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.arguments.model.common

import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.CommonCompilerArgument
import org.jetbrains.kotlin.buildtools.tests.arguments.model.ArgumentConfiguration
import org.jetbrains.kotlin.buildtools.tests.arguments.model.ArgumentTestDescriptor

internal class CommonArgumentConfiguration<T>(
    kotlinToolchain: KotlinToolchains,
    argumentTestDescriptor: ArgumentTestDescriptor<T>,
) : ArgumentConfiguration<T>(kotlinToolchain, argumentTestDescriptor) {
    val argumentKey: CommonCompilerArgument<T> =
        CommonCompilerArgument(argumentTestDescriptor.argumentId, argumentTestDescriptor.availableSinceVersion)
    val argumentValues: List<T> = argumentTestDescriptor.argumentValues
}