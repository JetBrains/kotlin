/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.arguments.model.common

import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.CommonCompilerArgument
import org.jetbrains.kotlin.buildtools.tests.arguments.model.ArgumentConfiguration

internal class CommonArgumentConfiguration<T>(
    kotlinToolchain: KotlinToolchains,
    commonArgumentTestDescriptor: CommonArgumentTestDescriptor<T>,
) : ArgumentConfiguration<T>(kotlinToolchain, commonArgumentTestDescriptor) {
    val argumentKey: CommonCompilerArgument<T> = commonArgumentTestDescriptor.argument
    val argumentValues: List<T> = commonArgumentTestDescriptor.argumentValues

    val invalidArgumentValues: List<T> = commonArgumentTestDescriptor.invalidArgumentValues
    val invalidRawValues: List<String> = commonArgumentTestDescriptor.invalidRawValues
}
