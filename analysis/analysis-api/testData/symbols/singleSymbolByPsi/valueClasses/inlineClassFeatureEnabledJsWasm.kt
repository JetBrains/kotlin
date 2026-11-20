// LANGUAGE: +FullValueClasses
// TARGET_PLATFORM: JS, Wasm
// WITH_STDLIB

import kotlin.jvm.JvmInline

@JvmInline
value class ValueClassJvmInline<caret>JsWasm(val value: Int)
