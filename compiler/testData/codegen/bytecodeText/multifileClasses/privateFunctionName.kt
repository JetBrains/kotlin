// WITH_RUNTIME
// FILE: part1.kt

@file:JvmName("Facade")
@file:JvmMultifileClass

package test

private fun privateFun() {}

private var privateVar = 42
    get() = field
    set(value) { field = value }

// We mangle names of private declarations in multi-file parts because in the -Xmultifile-parts-inherit mode, they can clash accidentally.
// Below, one usage of each declaration is at the declaration site, another in the Metadata.d2 array.
// 2 privateFun\$Facade__Part1Kt
// 2 getPrivateVar\$Facade__Part1Kt
// 2 setPrivateVar\$Facade__Part1Kt
