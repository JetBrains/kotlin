/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.linkage.partial

import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.lazy.IrLazyDeclarationBase
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.ir.linkage.partial.PartialLinkageUtils.File as PLFile

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

    /** Like [ClassId], but can be used for any declaration. */
    data class DeclarationId(val packageFqName: String, val declarationRelativeFqName: String) {
        private fun createNested(name: String) =
            DeclarationId(packageFqName, if (declarationRelativeFqName.isNotEmpty()) "$declarationRelativeFqName.$name" else name)

        override fun toString() = "$packageFqName/$declarationRelativeFqName"

        companion object {
            val IrDeclarationWithName.declarationId: DeclarationId?
                get() {
                    return when (val parent = parent) {
                        is IrPackageFragment -> DeclarationId(parent.packageFqName.asString(), name.asString())
                        is IrDeclarationWithName -> parent.declarationId?.createNested(name.asString())
                        else -> null
                    }
                }
        }
    }

    /**
     * Check if the outermost lazy IR class containing [this] declaration is private. If it is, which normally should not happen
     * because the lazy IR declaration referenced from non-lazy IR is always expected to be exported from its module and thus
     * to be non-private, then consider [this] as the effectively missing declaration.
     *
     * Such behaviour conforms with behavior that can be observed with no lazy IR usage. Consider the case:
     * 1. Module A.v1:
     *      public class A {
     *          fun foo(): String // ID signature: "/A.foo|123"
     *      }
     * 2. Module A.v2:
     *      private class A {
     *          fun foo(): String // private ID signature (as the containing class is private)
     *      }
     * 3. Module B -> A.v1:
     *      fun bar() {
     *          val a = A()
     *          a.foo() // IR call references a function symbol with ID signature: "/A.foo|123"
     *      }
     * 4. Module App -> A.v2, B
     *      // Invocation of `A.foo()` inside `fun bar()` is replaced by the IR linkage error. That's
     *      // because no declaration found for symbol "/A.foo|123" during the IR linkage phase.
     */
    fun IrLazyDeclarationBase.isEffectivelyMissingLazyIrDeclaration(): Boolean {
        val nearestClass = this as? IrClass ?: parentClassOrNull ?: return false
        val outermostClass = generateSequence(nearestClass) { it.parentClassOrNull }.last()
        return outermostClass.visibility == DescriptorVisibilities.PRIVATE
    }
}

/** An optimization to avoid re-computing file for every visited declaration */
internal abstract class FileAwareIrElementTransformerVoid(startingFile: PLFile?) : IrElementTransformerVoid() {
    private var _currentFile: PLFile? = startingFile
    val currentFile: PLFile get() = _currentFile ?: error("No information about current file")

    protected fun <T> runInFile(file: PLFile, block: () -> T): T {
        val previousFile = _currentFile
        _currentFile = file
        try {
            return block()
        } finally {
            _currentFile = previousFile
        }
    }

    final override fun visitFile(declaration: IrFile) = runInFile(PLFile.IrBased(declaration)) {
        super.visitFile(declaration)
    }
}
