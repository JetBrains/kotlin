/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.serialization

import org.jetbrains.kotlin.metadata.ProtoBuf

class FirAnnotationArgumentVisitorData(
    val serializer: FirAnnotationSerializer,
    val builder: ProtoBuf.Annotation.Argument.Value.Builder
) {
    val stringTable: FirElementAwareStringTable get() = serializer.stringTable
}