// IGNORE_BACKEND: ANY
// TARGET_BACKEND: JVM
// WITH_STDLIB

// MODULE: lib
// FILE: Lib.kt

class EntityID<T>

typealias EID = EntityID<Long>

// MODULE: main(lib)
// FILE: Main.kt

abstract class ChatHistoryWithDateResponse() : List<<!OUTER_CLASS_ARGUMENTS_REQUIRED!>EID<!>>

fun box() = "OK"
