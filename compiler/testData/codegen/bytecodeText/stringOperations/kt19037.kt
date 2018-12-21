// IGNORE_BACKEND: JVM_IR
const val empty = ""

val test1 = ""
val test2 = "abc$empty"
val test3 = "$empty$empty$empty"

// 0 StringBuilder