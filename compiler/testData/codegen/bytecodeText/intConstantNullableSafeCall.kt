// IGNORE_BACKEND: JVM_IR
val a : Int? = 10

fun foo() = a?.toString()

// 1 IFNULL
