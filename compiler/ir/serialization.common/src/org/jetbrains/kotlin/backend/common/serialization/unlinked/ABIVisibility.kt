/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization.unlinked

import org.jetbrains.kotlin.backend.common.serialization.unlinked.ABIVisibility.Module.IrBased.Companion.irModule
import org.jetbrains.kotlin.backend.common.serialization.unlinked.ABIVisibility.Module.LazyIrBased.Companion.moduleDescriptor
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.UNDEFINED_COLUMN_NUMBER
import org.jetbrains.kotlin.ir.UNDEFINED_LINE_NUMBER
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.lazy.IrLazyDeclarationBase
import org.jetbrains.kotlin.ir.util.IrMessageLogger
import org.jetbrains.kotlin.ir.util.fileOrNull
import org.jetbrains.kotlin.ir.util.getPackageFragment
import org.jetbrains.kotlin.ir.util.nameForIrSerialization
import org.jetbrains.kotlin.resolve.descriptorUtil.module

internal sealed interface ABIVisibility {
    /** A public declaration that is visible to the whole world. */
    object WholeWorld : ABIVisibility

    /** Not the whole world. */
    sealed interface Limited : ABIVisibility

    /**
     * A declaration is visible inside the module (plus friends) only.
     */
    sealed class Module(val name: String) : Limited {
        final override fun equals(other: Any?) = (other as? Module)?.name == name
        final override fun hashCode() = name.hashCode()

        abstract operator fun contains(fragment: IrModuleFragment): Boolean
        abstract operator fun contains(declaration: IrDeclaration): Boolean

        class IrBased(private val module: IrModuleFragment) : Module(module.name.asString()) {
            override fun contains(fragment: IrModuleFragment) = fragment == module
            override fun contains(declaration: IrDeclaration) = declaration.irModule == module

            companion object {
                inline val IrDeclaration.irModule: IrModuleFragment? get() = fileOrNull?.module
            }
        }

        class LazyIrBased(private val module: ModuleDescriptor) : Module(module.name.asString()) {
            override fun contains(fragment: IrModuleFragment) = fragment.descriptor == module
            override fun contains(declaration: IrDeclaration) = declaration.moduleDescriptor == module

            companion object {
                inline val IrDeclaration.moduleDescriptor: ModuleDescriptor? get() = (this as? IrLazyDeclarationBase)?.descriptor?.module
            }
        }

        object MissingDeclarations : Module("<missing declarations>") {
            override fun contains(fragment: IrModuleFragment) = false
            override fun contains(declaration: IrDeclaration) = false
        }

        companion object {
            fun determineModuleFor(declaration: IrDeclaration): Module {
                return if (declaration.origin == PartiallyLinkedDeclarationOrigin.MISSING_DECLARATION)
                    MissingDeclarations
                else
                    declaration.irModule?.let(::IrBased)
                        ?: declaration.moduleDescriptor?.let(::LazyIrBased)
                        ?: error("Can't determine module for $declaration, ${declaration.nameForIrSerialization}")
            }
        }
    }

    /**
     * A declaration that is visible only within the single file.
     */
    sealed interface File : Limited {
        val module: Module
        fun computeLocationForOffset(offset: Int): IrMessageLogger.Location

        data class IrBased(private val file: IrFile) : File {
            override val module = Module.IrBased(file.module)

            override fun computeLocationForOffset(offset: Int): IrMessageLogger.Location {
                val lineNumber = if (offset == UNDEFINED_OFFSET) UNDEFINED_LINE_NUMBER else file.fileEntry.getLineNumber(offset)
                val columnNumber = if (offset == UNDEFINED_OFFSET) UNDEFINED_COLUMN_NUMBER else file.fileEntry.getColumnNumber(offset)

                // TODO: should module name still be added here?
                return IrMessageLogger.Location("${module.name} @ ${file.fileEntry.name}", lineNumber, columnNumber)
            }
        }

        class LazyIrBased(externalPackageFragment: IrExternalPackageFragment) : File {
            private val location: IrMessageLogger.Location
            override val module: Module

            init {
                module = Module.LazyIrBased(externalPackageFragment.packageFragmentDescriptor.containingDeclaration)
                location = IrMessageLogger.Location(module.name, UNDEFINED_LINE_NUMBER, UNDEFINED_COLUMN_NUMBER)
            }

            override fun equals(other: Any?) = (other as? LazyIrBased)?.module == module
            override fun hashCode() = module.hashCode()

            override fun computeLocationForOffset(offset: Int) = location
        }

        object MissingDeclarations : File {
            private val location = IrMessageLogger.Location(Module.MissingDeclarations.name, UNDEFINED_LINE_NUMBER, UNDEFINED_COLUMN_NUMBER)

            override val module get() = Module.MissingDeclarations
            override fun computeLocationForOffset(offset: Int) = location
        }

        companion object {
            fun determineFileFor(declaration: IrDeclaration): File {
                return if (declaration.origin == PartiallyLinkedDeclarationOrigin.MISSING_DECLARATION)
                    MissingDeclarations
                else
                    when (val packageFragment = declaration.getPackageFragment()) {
                        is IrFile -> IrBased(packageFragment)
                        is IrExternalPackageFragment -> LazyIrBased(packageFragment)
                        else -> error("Can't determine file for $declaration, ${declaration.nameForIrSerialization}")
                    }
            }
        }
    }

    companion object {
        fun determineVisibilityFor(declaration: IrDeclaration): ABIVisibility {
            if (declaration !is IrDeclarationWithVisibility) {
                // Type parameters do not have own visibility. For the purpose of p.l. it's enough to consider them "public".
                return WholeWorld
            }

            return when (declaration.visibility) {
                DescriptorVisibilities.PUBLIC, DescriptorVisibilities.PROTECTED -> WholeWorld
                DescriptorVisibilities.INTERNAL -> Module.determineModuleFor(declaration)
                else -> File.determineFileFor(declaration)
            }
        }

        fun chooseNarrower(left: ABIVisibility, right: ABIVisibility): ABIVisibility? = when (left) {
            is WholeWorld -> right
            is Module -> when (right) {
                is WholeWorld -> left
                is Module -> if (left == right) left else null
                is File -> if (left == right.module) right else null
            }
            is File -> when (right) {
                is WholeWorld -> left
                is Module -> if (left.module == right) left else null
                is File -> if (left == right) left else null
            }
        }
    }
}
