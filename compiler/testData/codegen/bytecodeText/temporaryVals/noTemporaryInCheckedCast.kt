fun foo(): Any? = "abc"

fun test() = foo() as String

// 0 ASTORE
// 0 ALOAD
