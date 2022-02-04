/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend.jvm

import org.jetbrains.kotlin.backend.common.serialization.metadata.KlibMetadataVersion
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.backend.Fir2IrComponents
import org.jetbrains.kotlin.fir.serialization.FirSerializerExtension

class FirJsSerializerExtension(
    override val session: FirSession,
    components: Fir2IrComponents,
) : FirSerializerExtension() {
    override val stringTable = FirJsElementAwareStringTable(components)
    override val metadataVersion = KlibMetadataVersion.INSTANCE
}
