// ISSUE: KT-58754
// IGNORE_BACKEND_K1: ANY
// WITH_STDLIB


fun foo(): Int = 1
fun bar(): Int = 2

class Test(b: Boolean) {
    val test_1 by lazy {
        val a = if (b) {
            ::foo
        } else {
            ::bar
        }
        a
    }

    val test_2 by lazy {
        val a = if (b) ::foo else ::bar
        a
    }

    val test_3 by lazy {
        val a = when {
            b -> { ::foo }
            else -> { ::bar }
        }
        a
    }

    val test_4 by lazy {
        val a = when {
            b -> ::foo
            else -> ::bar
        }
        a
    }
}

fun box(): String {
    with(Test(b = false)) {
        require(test_1() == bar())
        require(test_2() == bar())
        require(test_3() == bar())
        require(test_4() == bar())
    }
    with(Test(b = true)) {
        require(test_1() == foo())
        require(test_2() == foo())
        require(test_3() == foo())
        require(test_4() == foo())
    }
    return "OK"
}
