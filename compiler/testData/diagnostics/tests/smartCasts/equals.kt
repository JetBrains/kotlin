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

fun string(foo: Any) {
    val string = ""
    if ("" == foo) <!DEBUG_INFO_SMARTCAST!>foo<!>.length
    if (string == foo) <!DEBUG_INFO_SMARTCAST!>foo<!>.length
    if (foo == "") foo.<!UNRESOLVED_REFERENCE!>length<!>
}

fun int(foo: Any) {
    val int = 1
    if (1 == foo) foo.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>plus<!>(1)
    if (int == foo) foo.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>plus<!>(1)
    if (foo == 1) foo.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>plus<!>(1)
}

fun long(foo: Any) {
    val long = 1L
    if (1L == foo) foo.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>plus<!>(1L)
    if (long == foo) foo.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>plus<!>(1L)
    if (foo == 1L) foo.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>plus<!>(1L)
}

fun char(foo: Any) {
    val char = 'a'
    if ('a' == foo) foo.<!UNRESOLVED_REFERENCE!>compareTo<!>('a')
    if (char == foo) foo.<!UNRESOLVED_REFERENCE!>compareTo<!>('a')
    if (foo == 'a') foo.<!UNRESOLVED_REFERENCE!>compareTo<!>('a')
}
