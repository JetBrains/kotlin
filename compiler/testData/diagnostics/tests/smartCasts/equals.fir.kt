fun foo(x: String?) = x

class Test

class TestWithEquals {
    override fun equals(other: Any?) = super.equals(other)
}

fun bar(i: Test?) {
    if (i == null) foo(<!ARGUMENT_TYPE_MISMATCH!>i<!>)
}

fun bar(i: TestWithEquals?) {
    if (i == null) foo(<!ARGUMENT_TYPE_MISMATCH!>i<!>)
    if (null == i) foo(<!ARGUMENT_TYPE_MISMATCH!>i<!>)
    when (i) {
        null -> foo(<!ARGUMENT_TYPE_MISMATCH!>i<!>)
    }
}

fun gav(i: TestWithEquals?, j: TestWithEquals?) {
    if (j == null) {
        if (i == j) foo(<!ARGUMENT_TYPE_MISMATCH!>i<!>)
    }
}