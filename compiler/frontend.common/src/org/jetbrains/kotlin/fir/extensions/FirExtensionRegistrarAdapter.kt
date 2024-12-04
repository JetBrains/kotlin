/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.extensions

import org.jetbrains.kotlin.extensions.ProjectExtensionDescriptor

/*
 * This is needed to prevent ClassNotFoundExceptions in IDE plugin built on FE 1.0
 *
 * Actual FirExtensionRegistrar uses some specific classes from FIR, so if we reference
 *   it in KotlinCoreEnvironment (where all extension points are registered) then we
 *   need to have all FIR compiler on classpath. And in IDE plugin we don't depend
 *   on FIR to reduce plugin size.
 */
abstract class FirExtensionRegistrarAdapter {
    companion object : ProjectExtensionDescriptor<FirExtensionRegistrarAdapter>(
        name = "org.jetbrains.kotlin.fir.extensions.firExtensionRegistrar",
        extensionClass = FirExtensionRegistrarAdapter::class.java
    )
}
