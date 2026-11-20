// LIBRARY_PLATFORMS: JVM
// JVM_TARGET: 17
// FILE: Declarations.kt
package test

class Foo {
    class Nested

    companion object
}

object Obj

interface Bar {
    companion object
}

fun topLevelFun() {}

val topLevelVal: Int = 0

// FILE: Part.kt
@file:[JvmName("Multifile") JvmMultifileClass]
package test

fun partFun() {}
