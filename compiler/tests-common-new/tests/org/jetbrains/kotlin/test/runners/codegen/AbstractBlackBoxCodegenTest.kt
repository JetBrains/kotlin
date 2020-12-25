/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.runners.codegen

import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.classic.ClassicJvmBackendFacade
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontend2ClassicBackendConverter
import org.jetbrains.kotlin.test.model.BackendFacade
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.Frontend2BackendConverter

open class AbstractBlackBoxCodegenTest : AbstractJvmBlackBoxCodegenTestBase(TargetBackend.JVM) {
    override val frontendToBackendConverter: Constructor<Frontend2BackendConverter<*, *>>
        get() = ::ClassicFrontend2ClassicBackendConverter

    override val backendFacade: Constructor<BackendFacade<*, BinaryArtifacts.Jvm>>
        get() = ::ClassicJvmBackendFacade
}
