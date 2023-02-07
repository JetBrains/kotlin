/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.web

import org.jetbrains.kotlin.backend.common.serialization.checkIsFunctionTypeInterfacePackageFqName
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment

class FunctionTypeInterfacePackages {
    companion object {
        private const val FUNCTION_TYPE_INTERFACE_DIR = "function-type-interface"
    }

    private val functionTypeInterfacePackageFiles = hashSetOf<IrFile>()

    // Function type interfaces are not declared in a standard library, and they are generated on flight during a klib deserialization.
    // The accessor allows finding a package for storing the generated function type interfaces.
    // The optimization reduces the size of a standard library klib.
    // Here are some numbers:
    //      255 interfaces in one package increase the size of uncompressed klib up to 3.5+MB;
    //      512 interfaces in one package increase the size of uncompressed klib up to 14.5+MB!
    // We need 3 packages.
    fun makePackageAccessor(stdlibModule: IrModuleFragment) = { packageFragmentDescriptor: PackageFragmentDescriptor ->
        val packageFqName = packageFragmentDescriptor.fqName.toString()
        check(checkIsFunctionTypeInterfacePackageFqName(packageFqName)) { "unexpected function type interface package $packageFqName" }

        val fileWithRequiredPackage = "${packageFqName.replace('.', '-')}-package.kt"
        val packageFile = stdlibModule.files.singleOrNull {
            // Do not check by name "$FUNCTION_TYPE_INTERFACE_DIR/$fileWithRequiredPackage" because the path separator depends on OS
            it.fileEntry.name.endsWith(fileWithRequiredPackage) && it.fileEntry.name.contains(FUNCTION_TYPE_INTERFACE_DIR)
        } ?: error("can not find a functional interface file for $packageFqName package")

        check(packageFragmentDescriptor.fqName == packageFile.fqName) {
            "unexpected package in file ${packageFile.fileEntry.name}; expected $packageFqName, got ${packageFile.fqName}"
        }

        functionTypeInterfacePackageFiles += packageFile
        packageFile
    }

    fun isFunctionTypeInterfacePackageFile(file: IrFile) = file in functionTypeInterfacePackageFiles
}
