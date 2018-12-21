// !LANGUAGE: +InlineClasses
// IGNORE_BACKEND: JVM_IR

// FILE: Z.kt
inline class Z(val value: Int)

// FILE: test.kt
data class Data(val z1: Z, val z2: Z)

inline class Inline(val z: Z)

// @Data.class:
// 0 Z.box
// 0 Z.unbox

// @Inline.class:
// 0 Z.box
// 0 Z.unbox
