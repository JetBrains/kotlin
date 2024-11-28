// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.kotlin.analysis.api.dumdum.stubindex

import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtCallableDeclaration

abstract class KotlinExtensionsByReceiverTypeStubIndexHelper : KotlinStringStubIndexHelper<KtCallableDeclaration>(
    KtCallableDeclaration::class.java
) {


    companion object {

        private const val SEPARATOR = '\n'

        data class Key(
            val receiverTypeName: Name,
            val callableName: Name,
        ) {

            constructor(
                receiverTypeIdentifier: String,
                callableIdentifier: String,
            ) : this(
                receiverTypeName = Name.identifier(receiverTypeIdentifier),
                callableName = Name.identifier(callableIdentifier),
            )

            constructor(key: String) : this(
                receiverTypeIdentifier = key.substringBefore(SEPARATOR, ""),
                callableIdentifier = key.substringAfter(SEPARATOR, ""),
            )

            val key: String
                get() = receiverTypeName.identifier + SEPARATOR + callableName.identifier
        }
    }
}