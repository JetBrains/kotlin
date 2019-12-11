fun foo(x: String?) = x

class Test

class TestWithEquals {
    override fun equals(other: Any?) = super.equals(other)
}

fun bar(i: Test?) {
    if (i == null) <!INAPPLICABLE_CANDIDATE!>foo<!>(i)
}

fun bar(i: TestWithEquals?) {
    if (i == null) <!INAPPLICABLE_CANDIDATE!>foo<!>(i)
    if (null == i) <!INAPPLICABLE_CANDIDATE!>foo<!>(i)
    when (i) {
        null -> <!INAPPLICABLE_CANDIDATE!>foo<!>(i)
    }
}

fun gav(i: TestWithEquals?, j: TestWithEquals?) {
    if (j == null) {
        if (i == j) <!INAPPLICABLE_CANDIDATE!>foo<!>(i)
    }
}