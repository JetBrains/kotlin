// JVM_TARGET: 1.8
// WITH_RUNTIME
// FULL_JDK

protocol interface Proto {
    fun foo(): String
    fun id(str: String): String
    fun sum(a: Int, b: Int): Int
}

class A {
    fun foo() = "OK"
    fun id(str: String) = str
    fun sum(a: Int, b: Int) = a + b
}

fun test(arg: Proto): String {
    if (arg.sum(2, 3) != 5) {
        return "FAIL"
    }

    if (arg.id("STR") != "STR") {
        return "FAIL"
    }

    return arg.foo()
}

fun box() = test(A())
