// SKIP_TXT

fun foo(x: String?) = x

class Test

class TestWithEquals {
    override fun equals(other: Any?) = super.equals(other)
}

fun bar(i: Test?) {
    if (i == null) foo(<!DEBUG_INFO_CONSTANT!>i<!>)
}

fun bar(i: TestWithEquals?) {
    if (i == null) foo(<!DEBUG_INFO_CONSTANT!>i<!>)
    if (null == i) foo(<!DEBUG_INFO_CONSTANT!>i<!>)
    when (i) {
        null -> foo(<!DEBUG_INFO_CONSTANT!>i<!>)
    }
}

fun gav(i: TestWithEquals?, j: TestWithEquals?) {
    if (j == null) {
        if (i == <!DEBUG_INFO_CONSTANT!>j<!>) foo(<!DEBUG_INFO_CONSTANT!>i<!>)
    }
}
