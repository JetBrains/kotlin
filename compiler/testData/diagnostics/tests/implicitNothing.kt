fun <!IMPLICIT_NOTHING_RETURN_TYPE!>foo<!>() = throw Exception()

fun <!IMPLICIT_NOTHING_RETURN_TYPE!>bar<!>() = null!!

fun <!IMPLICIT_NOTHING_RETURN_TYPE!>baz<!>() = bar()

fun gav(): Any = null!!

val <!IMPLICIT_NOTHING_PROPERTY_TYPE!>x<!> = null!!

val y: Nothing = throw Exception()

fun check() {
    // Error: KT-10449
    fun local() = bar()
    // Unreachable / unused, but not implicit Nothing
    <!UNREACHABLE_CODE!>val <!UNUSED_VARIABLE!>x<!> =<!> null!!
}
