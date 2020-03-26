package org.jetbrains.kotlin.konan.utils

import org.jetbrains.kotlin.builtins.konan.KonanBuiltIns
import org.jetbrains.kotlin.konan.util.KlibMetadataFactories
import org.jetbrains.kotlin.library.metadata.NativeTypeTransformer
import org.jetbrains.kotlin.library.metadata.NullFlexibleTypeDeserializer

/**
 * The default Kotlin/Native factories.
 */
object KonanFactories : KlibMetadataFactories({ KonanBuiltIns(it) }, NullFlexibleTypeDeserializer, NativeTypeTransformer())