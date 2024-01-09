/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.backend.jvm

import org.jetbrains.kotlin.bir.declarations.BirClass
import org.jetbrains.kotlin.bir.declarations.BirScript
import org.jetbrains.kotlin.bir.declarations.BirTypeParameter
import org.jetbrains.kotlin.bir.types.BirErrorType
import org.jetbrains.kotlin.bir.types.BirSimpleType
import org.jetbrains.kotlin.bir.types.BirType
import org.jetbrains.kotlin.bir.types.utils.classOrNull
import org.jetbrains.kotlin.bir.util.render

val BirTypeParameter.erasedUpperBound: BirClass
    get() {
        // Pick the (necessarily unique) non-interface upper bound if it exists
        for (type in superTypes) {
            val irClass = type.classOrNull?.owner ?: continue
            if (!irClass.isJvmInterface) return irClass
        }

        // Otherwise, choose either the first IrClass supertype or recurse.
        // In the first case, all supertypes are interface types and the choice was arbitrary.
        // In the second case, there is only a single supertype.
        return superTypes.first().erasedUpperBound
    }

val BirType.erasedUpperBound: BirClass
    get() = when (this) {
        is BirSimpleType -> when (val classifier = classifier.owner) {
            is BirClass -> classifier
            is BirTypeParameter -> classifier.erasedUpperBound
            is BirScript -> classifier.targetClass!!.owner
            else -> error(render())
        }
        is BirErrorType -> symbol.owner
        else -> error(render())
    }