/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.dce

import org.jetbrains.kotlin.backend.common.serialization.mangle.KotlinMangleComputer
import org.jetbrains.kotlin.backend.common.serialization.mangle.MangleMode
import org.jetbrains.kotlin.backend.common.serialization.mangle.ir.IrMangleComputer
import org.jetbrains.kotlin.backend.common.serialization.signature.PublicIdSignatureComputer
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.backend.js.lower.PrimaryConstructorLowering
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.AbstractJsManglerIr
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import java.io.File

private fun IrDeclaration.fallbackFqName(): String {
    val fqn = (this as? IrDeclarationWithName)?.fqNameWhenAvailable?.asString() ?: "<unknown>"
    val signature = when (this is IrFunction) {
        true -> this.valueParameters.joinToString(prefix = "(", postfix = ")") { it.type.dumpKotlinLike() }
        else -> ""
    }
    return fqn + signature
}

private fun IrDeclaration.getNameByGetter(getter: (IrDeclaration) -> String?): String {
    val signature = getter(this) ?: fallbackFqName()
    val instanceSignature = when (this.origin == IrDeclarationOrigin.FIELD_FOR_OBJECT_INSTANCE) {
        true -> "[for ${(this as? IrField)?.type?.classOrNull?.signature ?: "<unknown>"}]"
        else -> ""
    }
    val synthetic = when (this.origin.isSynthetic || this.origin == PrimaryConstructorLowering.SYNTHETIC_PRIMARY_CONSTRUCTOR) {
        true -> "[synthetic]"
        else -> ""
    }
    return signature + synthetic + instanceSignature
}


private val publicIdSignatureComputer = PublicIdSignatureComputer(object : AbstractJsManglerIr() {
    override fun getMangleComputer(mode: MangleMode, compatibleMode: Boolean): KotlinMangleComputer<IrDeclaration> {
        return IrMangleComputer(StringBuilder(256), mode, compatibleMode, allowOutOfScopeTypeParameters = true)
    }
})

internal fun IrDeclaration.getPublicSignature() = try {
    var signature: IdSignature? = null
    publicIdSignatureComputer.inFile(file.symbol) {
        signature = publicIdSignatureComputer.computeSignature(this)
    }
    signature?.render(IdSignatureRenderer.LEGACY)
} catch (err: RuntimeException) {
    null
}

internal fun IrDeclaration.fqNameForDceDump(): String = getNameByGetter { it.symbol.signature?.render() ?: it.getPublicSignature() }

internal fun IrDeclaration.fqNameForDisplayDceDump(): String = getNameByGetter(IrDeclaration::fallbackFqName)

internal data class IrDeclarationDumpInfo(val fqName: String, val displayName: String, val type: String, val size: Int)

fun dumpDeclarationIrSizesIfNeed(path: String?, allModules: List<IrModuleFragment>, dceDumpNameCache: DceDumpDeclarationStorage) {
    if (path == null) return

    val declarations = linkedSetOf<IrDeclarationDumpInfo>()

    allModules.forEach {
        it.acceptChildrenVoid(object : IrElementVisitorVoid {
            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            override fun visitDeclaration(declaration: IrDeclarationBase) {
                val type = when (declaration) {
                    is IrFunction -> "function"
                    is IrProperty -> "property"
                    is IrField -> "field"
                    is IrAnonymousInitializer -> "anonymous initializer"
                    else -> null
                }
                type?.let {
                    declarations.add(
                        IrDeclarationDumpInfo(
                            fqName = dceDumpNameCache.getOrPut(declaration).removeQuotes(),
                            displayName = declaration.fqNameForDisplayDceDump().removeQuotes(),
                            type = it,
                            size = declaration.dumpKotlinLike().length
                        )
                    )
                }

                super.visitDeclaration(declaration)
            }
        })
    }
    dumpIrDeclarationSizes(declarations, File(path))
}

fun dumpExtendedDeclarationsIrSizes(path: String?, dceDumpNameCache: DceDumpDeclarationStorage) {
    if (path == null) {
        return
    }
    val extendedDeclarations = dceDumpNameCache.allCachedDeclarations.map {
        val type = when (it) {
            is IrFunction -> "function"
            is IrProperty -> "property"
            is IrField -> "field"
            is IrAnonymousInitializer -> "anonymous initializer"
            is IrVariable -> "variable"
            is IrValueParameter -> "value parameter"
            is IrClass -> "class"
            else -> error("invalid IR declaration type ${it.render()}")
        }
        IrDeclarationDumpInfo(
            fqName = dceDumpNameCache.getOrPut(it).removeQuotes(),
            displayName = it.fqNameForDisplayDceDump().removeQuotes(),
            type = type,
            size = it.dumpKotlinLike().length
        )
    }
    dumpIrDeclarationSizes(extendedDeclarations, File(path))
}

internal fun dumpIrDeclarationSizes(declarations: Collection<IrDeclarationDumpInfo>, outputFile: File) {
    val (prefix, postfix, separator, indent) = when (outputFile.extension) {
        "json" -> listOf("{\n", "\n}", ",\n", "    ")
        "js" -> listOf("export const kotlinDeclarationsSize = {\n", "\n};\n", ",\n", "    ")
        else -> listOf("", "", "\n", "")
    }

    val value = declarations.joinToString(separator, prefix, postfix) { declaration ->
        """$indent"${declaration.fqName}": {
                |$indent$indent"size": ${declaration.size},
                |$indent$indent"type": "${declaration.type}",
                |$indent$indent"displayName": "${declaration.displayName}"
                |$indent}
            """.trimMargin()
    }
    outputFile.writeText(value)
}

internal fun String.removeQuotes() = replace('"'.toString(), "")
    .replace("'", "")
    .replace("\\", "\\\\")
