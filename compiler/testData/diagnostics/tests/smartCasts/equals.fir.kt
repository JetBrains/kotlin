// SKIP_TXT

fun foo(x: String?) = x

class Test

class TestWithEquals {
    override fun equals(other: Any?) = super.equals(other)
}

fun bar(i: Test?) {
    if (i == null) foo(i)
}

fun bar(i: TestWithEquals?) {
    if (i == null) foo(i)
    if (null == i) foo(i)
    when (i) {
        null -> foo(i)
    }
}

fun gav(i: TestWithEquals?, j: TestWithEquals?) {
    if (j == null) {
        if (i == j) foo(i)
    }
}
