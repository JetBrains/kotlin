package test.some

class Test
class GTest<T>

fun <T1: /*false*/test.some.Test,
        T2: test./*true*/some.Test,
        T3: test.some./*true*/Test,
        T: /*false*/Test> foo(a: /*false*/test.some.Test, b: test./*true*/some.Test, c: test.some./*true*/Test, d: /*false*/Test) {

    val t1: /*false*/test.some.Test? = null
    val t2: test./*true*/some.Test? = null
    val t3: test.some./*true*/Test? = null
    val t4: /*false*/Test? = null

    val t5: GTest</*false*/test.some.Test>? = null
    val t6: GTest<test./*true*/some.Test>? = null
    val t7: GTest<test.some./*true*/Test>? = null
    val t8: GTest</*false*/Test>? = null
    val t9: test.some.GTest</*false*/Test>? = null
    val t10: /*false*/GTest<Test>? = null
}