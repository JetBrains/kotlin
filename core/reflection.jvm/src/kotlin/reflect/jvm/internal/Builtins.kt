/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal

import kotlin.metadata.*

internal fun createFunctionKmClass(arity: Int): KmClass = KmClass().apply {
    name = "kotlin/Function$arity"
    kind = ClassKind.INTERFACE
    modality = Modality.ABSTRACT
    visibility = Visibility.PUBLIC

    for (i in 1..arity) {
        typeParameters.add(KmTypeParameter("P$i", i, KmVariance.IN))
    }
    val returnTypeParameterId = arity + 1
    typeParameters.add(KmTypeParameter("R", returnTypeParameterId, KmVariance.OUT))

    supertypes.add(KmType().apply {
        classifier = KmClassifier.Class("kotlin/Function")
        arguments.add(KmTypeProjection(KmVariance.INVARIANT, KmType().apply {
            classifier = KmClassifier.TypeParameter(returnTypeParameterId)
        }))
    })

    // TODO (KT-80710): `invoke` function (note that even though it's created in the old reflection, it causes a failure KT-42199.)
}
