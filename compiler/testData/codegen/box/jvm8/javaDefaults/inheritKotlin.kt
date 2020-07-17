// TARGET_BACKEND: JVM
// FILE: Simple.java

interface Simple extends KInterface {
    default String test() {
        return "simple";
    }
}

// FILE: main.kt
// JVM_TARGET: 1.8
interface KInterface {
    fun test(): String {
        return "base";
    }
}

class Test : Simple {
    fun bar(): String {
        return super.test()
    }
}

fun box(): String {
    val test = Test().test()
    if (test != "simple") return "fail $test"

    val bar = Test().bar()
    if (bar != "simple") return "fail 2 $bar"

    return "OK"
}
