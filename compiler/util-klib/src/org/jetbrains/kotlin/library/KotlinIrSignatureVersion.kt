/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library

/**
 * Describes how signatures of IR declarations are serialized in a KLIB.
 */
data class KotlinIrSignatureVersion(val number: Int) {

    companion object {

        /**
         * Signatures are represented by the [org.jetbrains.kotlin.ir.util.IdSignature] class. Signatures of public declarations
         * ([org.jetbrains.kotlin.ir.util.IdSignature.CommonSignature]) have no description.
         * Only their numeric identifier participates in linkage.
         */
        val V1 = KotlinIrSignatureVersion(1)

        /**
         * Like [V1], but signatures of public declarations have a description along with a numeric identifier that can be used for showing
         * more readable ABI dumps and linker errors.
         * Linkage is still performed using only the numeric identifier, thus making [V2] fully compatible with [V1].
         */
        val V2 = KotlinIrSignatureVersion(2)

        /**
         * The signature versions that this compiler emits.
         */
        val CURRENTLY_SUPPORTED_VERSIONS = setOf(V1, V2)
    }
}

internal fun String.parseIrSignatureVersions(): Set<KotlinIrSignatureVersion> =
    split(",")
        .mapTo(hashSetOf()) { KotlinIrSignatureVersion(it.toInt()) }

internal fun Set<KotlinIrSignatureVersion>.toManifestValue(): String =
    sortedBy { it.number }
        .joinToString(separator = ",") { it.number.toString() }

