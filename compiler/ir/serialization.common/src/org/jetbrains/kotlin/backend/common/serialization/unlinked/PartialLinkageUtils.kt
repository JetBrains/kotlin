/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization.unlinked

import org.jetbrains.kotlin.backend.common.serialization.unlinked.PartialLinkageUtils.Module.DescriptorBased.Companion.moduleDescriptor
import org.jetbrains.kotlin.backend.common.serialization.unlinked.PartialLinkageUtils.Module.IrBased.Companion.irModule
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.lazy.IrLazyDeclarationBase
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.fileOrNull
import org.jetbrains.kotlin.ir.util.nameForIrSerialization
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.module

internal object PartialLinkageUtils {
    val UNKNOWN_NAME = Name.identifier("<unknown name>")

    fun IdSignature.guessName(nameSegmentsToPickUp: Int): String? = when (this) {
        is IdSignature.CommonSignature -> if (nameSegmentsToPickUp == 1)
            shortName
        else
            nameSegments.takeLast(nameSegmentsToPickUp).joinToString(".")

        is IdSignature.CompositeSignature -> inner.guessName(nameSegmentsToPickUp)
        is IdSignature.AccessorSignature -> accessorSignature.guessName(nameSegmentsToPickUp)

        else -> null
    }

    /** For fast check if a declaration is in the module */
    internal sealed interface Module {
        operator fun contains(fragment: IrModuleFragment): Boolean
        operator fun contains(declaration: IrDeclaration): Boolean

        class IrBased(private val module: IrModuleFragment) : Module {
            override fun contains(fragment: IrModuleFragment) = fragment == module
            override fun contains(declaration: IrDeclaration) = declaration.irModule == module

            companion object {
                inline val IrDeclaration.irModule: IrModuleFragment? get() = fileOrNull?.module
            }
        }

        class DescriptorBased(private val module: ModuleDescriptor) : Module {
            override fun contains(fragment: IrModuleFragment) = fragment.descriptor == module
            override fun contains(declaration: IrDeclaration) = declaration.moduleDescriptor == module

            companion object {
                inline val IrDeclaration.moduleDescriptor: ModuleDescriptor? get() = if (this is IrLazyDeclarationBase) descriptor.module else null
            }
        }

        companion object {
            fun determineFor(declaration: IrDeclaration): Module {
                return declaration.irModule?.let(::IrBased)
                    ?: declaration.moduleDescriptor?.let(::DescriptorBased)
                    ?: error("Can't compute module for $declaration, ${declaration.nameForIrSerialization}")
            }
        }
    }
}
