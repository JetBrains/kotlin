// !LANGUAGE: +MultiPlatformProjects
// MODULE: m1-common
// FILE: common.kt
header enum class Foo { A, B }
header enum class Bar { X, Y, Z }

// MODULE: m2-jvm(m1-common)
// FILE: jvm.kt
impl enum class Foo { A, B, C, D, E }
impl enum class Bar { V, X, W, Y, Z }
