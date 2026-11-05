// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +MultiPlatformProjects +FullValueClasses
// MODULE: commonjs
// FILE: commonjs.kt

import kotlin.jvm.JvmInline

@JvmInline
expect value class Val(val x: Int)

expect value class ForRef(val s: String)

// MODULE: js()()(commonjs)
// FILE: js.kt

actual inline value class Val(val x: Int)

actual inline value class <!EXPECT_ACTUAL_INCOMPATIBLE_CLASS_MODIFIERS!>ForRef<!>(val s: String)
