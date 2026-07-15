/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.forward.tests.arguments.model.metadata

import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.arguments.MetadataArguments.MetadataArgument
import org.jetbrains.kotlin.buildtools.forward.tests.arguments.model.ArgumentConfiguration

internal class MetadataArgumentConfiguration<T>(
    kotlinToolchain: KotlinToolchains,
    metadataArgumentTestDescriptor: MetadataArgumentTestDescriptor<T>,
) : ArgumentConfiguration<T>(kotlinToolchain, metadataArgumentTestDescriptor) {
    val argumentKey: MetadataArgument<T> = metadataArgumentTestDescriptor.argument
    val argumentValues: List<T> = metadataArgumentTestDescriptor.argumentValues
    val argumentRawValues: List<String> = metadataArgumentTestDescriptor.argumentRawValues

    val invalidArgumentValues: List<T> = metadataArgumentTestDescriptor.invalidArgumentValues
    val invalidRawValues: List<String> = metadataArgumentTestDescriptor.invalidRawValues
}
