/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test

import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.directives.model.SimpleDirective
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.defaultsProvider
import org.jetbrains.kotlin.utils.addToStdlib.shouldNotBeCalled

val TestServices.loadedMetadataSuppressionDirective: SimpleDirective
    get() = when (defaultsProvider.defaultFrontend) {
        FrontendKinds.ClassicFrontend -> CodegenTestDirectives.IGNORE_FIR_METADATA_LOADING_K1
        FrontendKinds.FIR -> CodegenTestDirectives.IGNORE_FIR_METADATA_LOADING_K2
        else -> shouldNotBeCalled()
    }