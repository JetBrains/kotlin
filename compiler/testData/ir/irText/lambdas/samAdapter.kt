// FIR_IDENTICAL
// WITH_STDLIB
// TARGET_BACKEND: JVM

// NO_SIGNATURE_DUMP
// ^KT-57428

fun test1() {
    val hello = Runnable { println("Hello, world!") }
    hello.run()
}
