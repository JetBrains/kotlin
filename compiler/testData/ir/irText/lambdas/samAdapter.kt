// FIR_IDENTICAL
// WITH_RUNTIME

fun test1() {
    val hello = Runnable { println("Hello, world!") }
    hello.run()
}