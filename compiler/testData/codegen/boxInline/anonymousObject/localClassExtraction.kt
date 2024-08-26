// MODULE: lib
// FILE: lib.kt

inline fun publicInlineMethod(): Any = object {}

inline fun publicInlineMethodWithCrossinline(crossinline f: () -> Unit): Any = object {
    fun run() = f()
}

inline fun publicInlineMethod2() = publicInlineMethodWithCrossinline {}

// FILE: lib-test.kt
fun libtest1(): String = publicInlineMethod()::class.toString()
fun libtest2(): String = publicInlineMethod()::class.simpleName!!
inline fun libtest3(): String = publicInlineMethod()::class.simpleName!!
fun libtest4(): String = libtest3()
fun libtest5(): String = publicInlineMethod2()::class.simpleName!!
fun libtest6(): String = publicInlineMethod2()::class.simpleName!!
inline fun libtest7(): String = publicInlineMethod2()::class.simpleName!!

fun libtest8(): String = libtest7()

// MODULE: main(lib)
// FILE: main-test.kt
fun maintest1(): String = publicInlineMethod()::class.simpleName!!
fun maintest2(): String = publicInlineMethod()::class.simpleName!!
inline fun maintest3(): String = publicInlineMethod()::class.simpleName!!
fun maintest4(): String = maintest3()
fun maintest5(): String = publicInlineMethod2()::class.simpleName!!
fun maintest6(): String = publicInlineMethod2()::class.simpleName!!
inline fun maintest7(): String = publicInlineMethod2()::class.simpleName!!
fun maintest8(): String = maintest7()

// FILE: main.kt
fun assertEquals(expected: Any, actual: Any) {
    if (expected != actual) throw AssertionError("$expected expected, got $actual")
}

fun box() {
    assertEquals("", libtest1())
    assertEquals("", libtest2())
    assertEquals("", libtest3())
    assertEquals("", libtest4())
    assertEquals("", libtest5())
    assertEquals("", libtest6())
    assertEquals("", libtest7())

    assertEquals("", maintest1())
    assertEquals("", maintest2())
    assertEquals("", maintest3())
    assertEquals("", maintest4())
    assertEquals("", maintest5())
    assertEquals("", maintest6())
    assertEquals("", maintest7())
}