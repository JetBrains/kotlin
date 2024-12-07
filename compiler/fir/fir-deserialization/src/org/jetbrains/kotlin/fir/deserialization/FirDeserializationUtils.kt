/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.deserialization

import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.utils.compilerPluginMetadata
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.VersionRequirement
import org.jetbrains.kotlin.protobuf.GeneratedMessageLite
import org.jetbrains.kotlin.protobuf.MessageLiteOrBuilder

/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

internal fun VersionRequirement.Companion.create(proto: MessageLiteOrBuilder, context: FirDeserializationContext): List<VersionRequirement> =
    create(proto, context.nameResolver, context.versionRequirementTable)

internal inline fun <M : GeneratedMessageLite.ExtendableMessage<M>> FirDeclaration.deserializeCompilerPluginMetadata(
    context: FirDeserializationContext,
    proto: M,
    getCompilerPluginMetadataList: M.() -> List<ProtoBuf.CompilerPluginData>
) {
    proto.getCompilerPluginMetadataList().takeIf { it.isNotEmpty() }?.let { pluginDataList ->
        this.compilerPluginMetadata = pluginDataList.associateBy(
            keySelector = { context.nameResolver.getString(it.pluginId) },
            valueTransform = { it.data.toByteArray() }
        )
    }
}
