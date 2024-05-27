// TARGET_BACKEND: JVM
// WITH_STDLIB

// MODULE: lib
// FILE: Lib.kt

class EntityID<T>

typealias EID = EntityID<Long>

// MODULE: main(lib)
// FILE: Main.kt

abstract class ChatHistoryWithDateResponse() : List<EID>

fun box() = "OK"
