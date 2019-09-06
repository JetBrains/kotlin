package org.jetbrains.kotlin.konan.utils

import org.jetbrains.kotlin.builtins.konan.KonanBuiltIns
import org.jetbrains.kotlin.konan.util.KlibMetadataFactories
import org.jetbrains.kotlin.serialization.konan.NullFlexibleTypeDeserializer
import org.jetbrains.kotlin.storage.StorageManager

fun createKonanBuiltIns(storageManager: StorageManager) = KonanBuiltIns(storageManager)
/**
 * The default Kotlin/Native factories.
 */
object KonanFactories : KlibMetadataFactories(::createKonanBuiltIns, NullFlexibleTypeDeserializer)