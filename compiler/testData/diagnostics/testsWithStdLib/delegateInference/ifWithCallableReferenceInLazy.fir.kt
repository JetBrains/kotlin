// ISSUE: KT-58754

fun foo() {}
fun bar() {}

class Test(b: Boolean) {
    private val test_1 by lazy {
        val a = if (b) {
            ::foo
        } else {
            ::bar
        }
        a
    }

    private val test_2 by lazy {
        val a = if (b) ::foo else ::bar
        a
    }

    private val test_3 by lazy {
        val a = when {
            b -> { ::foo }
            else -> { ::bar }
        }
        a
    }

    private val test_4 by lazy {
        val a = when {
            b -> ::foo
            else -> ::bar
        }
        a
    }
}

