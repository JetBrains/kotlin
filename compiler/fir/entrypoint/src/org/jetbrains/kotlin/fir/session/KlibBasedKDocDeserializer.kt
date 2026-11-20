/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.session

import org.jetbrains.kotlin.fir.deserialization.FirKDocDeserializer
import org.jetbrains.kotlin.library.metadata.KlibMetadataProtoBuf
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.getExtensionOrNull

object KlibBasedKDocDeserializer : FirKDocDeserializer {
    override fun loadPropertyKDoc(proto: ProtoBuf.Property): String? {
        return proto.getExtensionOrNull(KlibMetadataProtoBuf.propertyKdoc)
    }

    override fun loadPropertyGetterKDoc(proto: ProtoBuf.Property): String? {
        return proto.getExtensionOrNull(KlibMetadataProtoBuf.propertyGetterKdoc)
    }

    override fun loadPropertySetterKDoc(proto: ProtoBuf.Property): String? {
        return proto.getExtensionOrNull(KlibMetadataProtoBuf.propertySetterKdoc)
    }

    override fun loadFunctionKDoc(proto: ProtoBuf.Function): String? {
        return proto.getExtensionOrNull(KlibMetadataProtoBuf.functionKdoc)
    }

    override fun loadConstructorKDoc(proto: ProtoBuf.Constructor): String? {
        return proto.getExtensionOrNull(KlibMetadataProtoBuf.constructorKdoc)
    }

    override fun loadClassKDoc(proto: ProtoBuf.Class): String? {
        return proto.getExtensionOrNull(KlibMetadataProtoBuf.classKdoc)
    }

    override fun loadTypeAliasKDoc(proto: ProtoBuf.TypeAlias): String? {
        return proto.getExtensionOrNull(KlibMetadataProtoBuf.typeAliasKdoc)
    }

    override fun loadEnumEntryKDoc(proto: ProtoBuf.EnumEntry): String? {
        return proto.getExtensionOrNull(KlibMetadataProtoBuf.enumEntryKdoc)
    }
}