// FIR_IDENTICAL
// WITH_RUNTIME
// TARGET_BACKEND: JVM

fun test1() {
    val hello = Runnable { println("Hello, world!") }
    hello.run()
}