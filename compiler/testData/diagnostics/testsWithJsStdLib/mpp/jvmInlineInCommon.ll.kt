// LL_FIR_DIVERGENCE
//   Session for which we run `OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE` check is
//   `isCommon` for compiler tests, but `!isCommon` for AA ones.
// LL_FIR_DIVERGENCE
// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +MultiPlatformProjects +FullValueClasses
// MODULE: commonjs
// FILE: commonjs.kt

import kotlin.jvm.JvmInline

@<!OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE!>JvmInline<!>
expect value class Val(val x: Int)

expect value class ForRef(val s: String)

// MODULE: js()()(commonjs)
// FILE: js.kt

actual inline value class Val(val x: Int)

actual inline value class <!EXPECT_ACTUAL_INCOMPATIBLE_CLASS_MODIFIERS!>ForRef<!>(val s: String)
