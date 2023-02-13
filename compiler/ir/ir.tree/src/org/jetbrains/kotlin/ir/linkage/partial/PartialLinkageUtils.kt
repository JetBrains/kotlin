/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.linkage.partial

import org.jetbrains.kotlin.builtins.FunctionInterfacePackageFragment
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.ir.*
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.declarations.IrExternalPackageFragment
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrContainerExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin.PARTIAL_LINKAGE_RUNTIME_ERROR
import org.jetbrains.kotlin.ir.util.IrMessageLogger
import org.jetbrains.kotlin.ir.util.getPackageFragment
import org.jetbrains.kotlin.name.Name

fun IrStatement.isPartialLinkageRuntimeError(): Boolean {
    return when (this) {
        is IrCall -> origin == PARTIAL_LINKAGE_RUNTIME_ERROR //|| symbol == builtIns.linkageErrorSymbol
        is IrContainerExpression -> origin == PARTIAL_LINKAGE_RUNTIME_ERROR || statements.any { it.isPartialLinkageRuntimeError() }
        else -> false
    }
}

object PartialLinkageUtils {
    /** For fast check if a declaration is in the module */
    sealed interface Module {
        val name: String

        data class Real(override val name: String) : Module {
            constructor(name: Name) : this(name.asString())
        }

        object SyntheticBuiltInFunctions : Module {
            override val name = "<synthetic built-in functions>"
        }

        object MissingDeclarations : Module {
            override val name = "<missing declarations>"
        }

        fun defaultLocationWithoutPath() = IrMessageLogger.Location(name, UNDEFINED_LINE_NUMBER, UNDEFINED_COLUMN_NUMBER)

        companion object {
            fun determineModuleFor(declaration: IrDeclaration): Module = determineFor(
                declaration,
                onMissingDeclaration = MissingDeclarations,
                onSyntheticBuiltInFunction = SyntheticBuiltInFunctions,
                onIrBased = { Real(it.module.name) },
                onLazyIrBased = { Real(it.containingDeclaration.name) },
                onError = { error("Can't determine module for $declaration, name=${(declaration as? IrDeclarationWithName)?.name}") }
            )
        }
    }

    sealed interface File {
        val module: Module
        fun computeLocationForOffset(offset: Int): IrMessageLogger.Location

        data class IrBased(private val file: IrFile) : File {
            override val module = Module.Real(file.module.name)

            override fun computeLocationForOffset(offset: Int): IrMessageLogger.Location {
                val lineNumber = if (offset == UNDEFINED_OFFSET) UNDEFINED_LINE_NUMBER else file.fileEntry.getLineNumber(offset) + 1 // since humans count from 1, not 0
                val columnNumber = if (offset == UNDEFINED_OFFSET) UNDEFINED_COLUMN_NUMBER else file.fileEntry.getColumnNumber(offset) + 1

                // TODO: should module name still be added here?
                return IrMessageLogger.Location("${module.name} @ ${file.fileEntry.name}", lineNumber, columnNumber)
            }
        }

        class LazyIrBased(packageFragmentDescriptor: PackageFragmentDescriptor) : File {
            override val module = Module.Real(packageFragmentDescriptor.containingDeclaration.name)
            private val defaultLocation = module.defaultLocationWithoutPath()

            override fun equals(other: Any?) = (other as? LazyIrBased)?.module == module
            override fun hashCode() = module.hashCode()

            override fun computeLocationForOffset(offset: Int) = defaultLocation
        }

        object SyntheticBuiltInFunctions : File {
            override val module = Module.SyntheticBuiltInFunctions
            private val defaultLocation = module.defaultLocationWithoutPath()

            override fun computeLocationForOffset(offset: Int) = defaultLocation
        }

        object MissingDeclarations : File {
            override val module = Module.MissingDeclarations
            private val defaultLocation = module.defaultLocationWithoutPath()

            override fun computeLocationForOffset(offset: Int) = defaultLocation
        }

        companion object {
            fun determineFileFor(declaration: IrDeclaration): File = determineFor(
                declaration,
                onMissingDeclaration = MissingDeclarations,
                onSyntheticBuiltInFunction = SyntheticBuiltInFunctions,
                onIrBased = ::IrBased,
                onLazyIrBased = ::LazyIrBased,
                onError = { error("Can't determine file for $declaration, name=${(declaration as? IrDeclarationWithName)?.name}") }
            )
        }
    }

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    private inline fun <R> determineFor(
        declaration: IrDeclaration,
        onMissingDeclaration: R,
        onSyntheticBuiltInFunction: R,
        onIrBased: (IrFile) -> R,
        onLazyIrBased: (PackageFragmentDescriptor) -> R,
        onError: () -> Nothing
    ): R {
        return if (declaration.origin == PartiallyLinkedDeclarationOrigin.MISSING_DECLARATION)
            onMissingDeclaration
        else {
            val packageFragment = declaration.getPackageFragment()
            val packageFragmentDescriptor = with(packageFragment.symbol) { if (hasDescriptor) descriptor else null }

            when {
                packageFragmentDescriptor is FunctionInterfacePackageFragment -> onSyntheticBuiltInFunction
                packageFragment is IrFile -> onIrBased(packageFragment)
                packageFragment is IrExternalPackageFragment && packageFragmentDescriptor != null -> onLazyIrBased(packageFragmentDescriptor)
                else -> onError()
            }
        }
    }
}
