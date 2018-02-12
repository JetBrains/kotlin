package codegen.mpp.mpp_default_args

import kotlin.test.*

@Test fun runTest() {
    box()
}

fun box() {
    assertEquals(test1(), 42)
    assertEquals(test2(17), 34)
    assertEquals(test3(), -1)

    Test4().test()
    Test5().Inner().test()

    42.test6()
}

expect fun test1(x: Int = 42): Int
actual fun test1(x: Int) = x

expect fun test2(x: Int, y: Int = x): Int
actual fun test2(x: Int, y: Int) = x + y

expect fun test3(x: Int = 42, y: Int = x + 1): Int
actual fun test3(x: Int, y: Int) = x - y

expect class Test4 {
    fun test(arg: Any = this)
}

actual class Test4 {
    actual fun test(arg: Any) {
        assertEquals(arg, this)
    }
}

expect class Test5 {
    inner class Inner {
        constructor(arg: Any = this@Test5)

        fun test(arg1: Any = this@Test5, arg2: Any = this@Inner)
    }
}

actual class Test5 {
    actual inner class Inner {
        actual constructor(arg: Any) {
            assertEquals(arg, this@Test5)
        }

        actual fun test(arg1: Any, arg2: Any) {
            assertEquals(arg1, this@Test5)
            assertEquals(arg2, this@Inner)
        }
    }
}

expect fun Int.test6(arg: Int = this)
actual fun Int.test6(arg: Int) {
    assertEquals(arg, this)
}
