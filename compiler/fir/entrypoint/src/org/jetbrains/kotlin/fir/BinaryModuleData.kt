/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.name.Name

class BinaryModuleData(
    val regular: FirBinaryDependenciesModuleData,
    val dependsOn: FirBinaryDependenciesModuleData,
    val friend: FirBinaryDependenciesModuleData
) {
    companion object {
        fun createDependencyModuleData(
            name: Name,
            capabilities: FirModuleCapabilities = FirModuleCapabilities.Empty
        ): FirBinaryDependenciesModuleData {
            return FirBinaryDependenciesModuleData(name, capabilities)
        }

        fun initialize(mainModuleName: Name): BinaryModuleData {
            fun createData(name: String): FirBinaryDependenciesModuleData =
                createDependencyModuleData(Name.special(name))

            return BinaryModuleData(
                createData("<regular dependencies of $mainModuleName>"),
                createData("<dependsOn dependencies of $mainModuleName>"),
                createData("<friends dependencies of $mainModuleName>")
            )
        }
    }
}
