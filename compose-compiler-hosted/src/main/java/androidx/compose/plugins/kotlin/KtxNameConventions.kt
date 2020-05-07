package androidx.compose.plugins.kotlin

import org.jetbrains.kotlin.name.Name

object KtxNameConventions {
    val COMPOSER = Name.identifier("composer")
    val COMPOSER_PARAMETER = Name.identifier("\$composer")
    val KEY_PARAMETER = Name.identifier("\$key")
    val CHANGED_PARAMETER = Name.identifier("\$changed")
    val DEFAULT_PARAMETER = Name.identifier("\$default")
    val EMIT = Name.identifier("emit")
    val JOINKEY = Name.identifier("joinKey")
    val STARTRESTARTGROUP = Name.identifier("startRestartGroup")
    val ENDRESTARTGROUP = Name.identifier("endRestartGroup")
    val UPDATE_SCOPE = Name.identifier("updateScope")

    val EMIT_KEY_PARAMETER = Name.identifier("key")
    val EMIT_CTOR_PARAMETER = Name.identifier("ctor")
    val EMIT_UPDATER_PARAMETER = Name.identifier("update")
    val EMIT_CHILDREN_PARAMETER = Name.identifier("children")

    val CALL_KEY_PARAMETER = Name.identifier("key")
    val CALL_CTOR_PARAMETER = Name.identifier("ctor")
    val CALL_INVALID_PARAMETER = Name.identifier("invalid")
    val CALL_BLOCK_PARAMETER = Name.identifier("block")
}