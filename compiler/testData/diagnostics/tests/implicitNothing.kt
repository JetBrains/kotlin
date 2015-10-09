fun <!IMPLICIT_NOTHING_RETURN_TYPE!>foo<!>() = throw Exception()

fun <!IMPLICIT_NOTHING_RETURN_TYPE!>bar<!>() = null!!

fun <!IMPLICIT_NOTHING_RETURN_TYPE!>baz<!>() = bar()

fun gav(): Any = null!!
