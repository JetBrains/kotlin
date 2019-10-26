/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler.util

import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeAliasSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.render

object ImportResolverUtils {
    private inline fun buildTrimEnd(fn: StringBuilder.() -> Unit): String =
        buildString(fn).trimEnd()

    private inline fun <T> T.runTrimEnd(fn: T.() -> String): String =
        run(fn).trimEnd()

    private fun IrType.obtainImport() = obtainTypeImport()

    private fun IrType.obtainTypeImport() =
        when (this) {
            is IrDynamicType -> setOf()
            is IrSimpleType -> HashSet<String>().apply {
                add(classifier.renderClassifierFqn())
                arguments.forEach {
                    when (it) {
                        is IrTypeProjection -> it.type.classifierOrNull?.renderClassifierFqn()
                        else -> null
                    }?.let { it1 ->
                        add(
                            it1
                        )
                    }
                }
            }
            else -> setOf(javaClass.simpleName)
        }

    internal fun IrClassifierSymbol.renderClassifierFqn(): String =
        if (isBound)
            when (val owner = owner) {
                is IrClass -> owner.renderClassFqn()
                is IrTypeParameter -> owner.renderTypeParameterFqn()
                else -> "`unexpected classifier: ${owner.render()}`"
            }
        else
            "<unbound ${this.javaClass.simpleName}>"

    internal fun IrTypeAliasSymbol.renderTypeAliasFqn(): String =
        if (isBound)
            StringBuilder().also { owner.renderDeclarationFqn(it) }.toString()
        else
            "<unbound $this: ${this.descriptor}>"

    internal fun IrClass.renderClassFqn(): String =
        StringBuilder().also { renderDeclarationFqn(it) }.toString()

    internal fun IrTypeParameter.renderTypeParameterFqn(): String =
        StringBuilder().also { sb ->
            sb.append(name.asString())
            sb.append(" of ")
            renderDeclarationParentFqn(sb)
        }.toString()

    private fun IrDeclaration.renderDeclarationFqn(sb: StringBuilder) {
        renderDeclarationParentFqn(sb)
        sb.append('.')
        if (this is IrDeclarationWithName) {
            sb.append(name.asString())
        } else {
            sb.append(this)
        }
    }

    private fun IrDeclaration.renderDeclarationParentFqn(sb: StringBuilder) {
        try {
            val parent = this.parent
            if (parent is IrDeclaration) {
                parent.renderDeclarationFqn(sb)
            } else if (parent is IrPackageFragment) {
                sb.append(parent.fqName.toString())
            }
        } catch (e: UninitializedPropertyAccessException) {
            sb.append("<uninitialized parent>")
        }
    }

}