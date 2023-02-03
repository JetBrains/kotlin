/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.session

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.library.metadata.KlibMetadataSerializerProtocol

class KlibBasedAnnotationDeserializer(session: FirSession) :
    AbstractAnnotationDeserializerWithTypeAnnotations(session, KlibMetadataSerializerProtocol)
