// METADATA_TARGET_PLATFORMS: JS, WasmJs
// LANGUAGE: +MultiPlatformProjects
// OPT_IN: kotlin.js.ExperimentalJsExport
// WITH_STDLIB
// ISSUE: KT-84475
// MODULE: lib-common
// FILE: common.kt
import kotlin.js.JsExport

@JsExport
interface ZCharset {
    val name: String
    val aliases: Array<String>
}

@JsExport
class Key<T>

@JsExport
interface ZDataHolder {
    fun <T> getData(key: Key<T>): T?
    fun <T> putData(key: Key<T>, value: T?)
}

// MODULE: app-common(lib-common)
import kotlin.js.JsExport
import kotlin.js.JsName

@JsExport
interface ZAsyncSocket : ZDataHolder {
    val charset: ZCharset
    fun readBytes(): ByteArray
}
