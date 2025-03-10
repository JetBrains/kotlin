// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-74809
// WITH_STDLIB

fun writeTo(): Boolean = false

fun foo() {
    val _ = writeTo()
    val (a, _) = 1 to 2
    val (_) = 'a' to 'b'

    (<!SYNTAX!><!>val f = <!UNRESOLVED_REFERENCE!>first<!><!SYNTAX!>, val _ = second) = "first" to "second"<!>

    when(val _ = writeTo()) {
        true -> {}
        false -> {}
    }

    for (_ in 1..10) {}

    val _ = object {
        val <!UNDERSCORE_IS_RESERVED!>_<!> = <!UNRESOLVED_REFERENCE!>call<!>()
    }

    val _ by lazy { 10 }
    var _ = writeTo()
}

class Foo() {
    val <!UNDERSCORE_IS_RESERVED!>_<!> = <!UNRESOLVED_REFERENCE!>initMe<!>()
}

class Foo2() {
    init {
        val _ = <!UNRESOLVED_REFERENCE!>initMe<!>()
    }
}

val <!UNDERSCORE_IS_RESERVED!>_<!> = writeTo()
