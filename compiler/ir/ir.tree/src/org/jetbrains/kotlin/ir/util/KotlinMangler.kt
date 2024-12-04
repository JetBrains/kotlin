/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.ir.declarations.IrDeclaration

interface KotlinMangler<D : Any> {

    val String.hashMangle: Long

    fun D.isExported(compatibleMode: Boolean): Boolean

    /**
     * Returns the mangled name for the declaration [D] to be used for computing that declaration's [IdSignature].
     *
     * Unlike the one computed by [mangleString], this mangled name does not include mangled names of the declaration's parents.
     *
     * For example, for `foo`'s getter in the following code:
     * ```kotlin
     * class Test {
     *     val foo: Int
     * }
     * ```
     * the result of this function will be `"<get-foo>(){}kotlin.Int"` or `"<get-foo>(){}"` (depending on the target platform).
     *
     * **The result of this function affects klib ABI.**
     *
     * @param compatibleMode If `true`, the mangled names of property backing fields are just those fields' names.
     * Otherwise, mangles such fields exactly as their corresponding properties.
     */
    fun D.signatureString(compatibleMode: Boolean): String

    /**
     * Computes the hash code of the string returned by [signatureString] using the CityHash64 algorithm.
     *
     * This hash code is to be used for building the declaration's [IdSignature].
     *
     * **The result of this function affects klib ABI.**
     *
     * @param compatibleMode If `true`, the mangled names of property backing fields are just those fields' names.
     * Otherwise, mangles such fields exactly as their corresponding properties.
     *
     * @see [IdSignature.CommonSignature.id]
     */
    fun D.signatureMangle(compatibleMode: Boolean): Long = signatureString(compatibleMode).hashMangle

    fun D.isPlatformSpecificExport(): Boolean = false

    val manglerName: String

    interface DescriptorMangler : KotlinMangler<DeclarationDescriptor> {
        override val manglerName: String
            get() = "Descriptor"
    }

    interface IrMangler : KotlinMangler<IrDeclaration> {

        /**
         * Returns the mangled name for the declaration, prefixed by mangled names of all its parents.
         *
         * For example, for `foo`'s getter in the following code:
         * ```kotlin
         * class Test {
         *     val foo: Int
         * }
         * ```
         * the result of this function will be `"Test#<get-foo>(){}kotlin.Int"` or `"Test#<get-foo>(){}"`
         * (depending on the target platform).
         *
         * The result of this function is only used for assigning names to binary symbols of Kotlin functions in the final executable
         * produced by Kotlin/Native.
         * **It does not affect klib ABI.**
         *
         * @param compatibleMode If `true`, the mangled names of property backing fields are just those fields' names.
         * Otherwise, mangles such fields exactly as their corresponding properties.
         */
        fun IrDeclaration.mangleString(compatibleMode: Boolean): String

        override val manglerName: String
            get() = "Ir"
    }
}
