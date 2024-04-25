// DIAGNOSTICS: -UNUSED_VARIABLE

fun example() {
    val a = if (true) true else false
    val b = <!INVALID_IF_AS_EXPRESSION!>if<!> (true) else false
    val c = <!INVALID_IF_AS_EXPRESSION!>if<!> (true) true
    val d = <!INVALID_IF_AS_EXPRESSION!>if<!> (true) true else;
    val e = if (true) <!IMPLICIT_CAST_TO_ANY!>{}<!> else <!IMPLICIT_CAST_TO_ANY!>false<!>
    val f = if (true) <!IMPLICIT_CAST_TO_ANY!>true<!> else <!IMPLICIT_CAST_TO_ANY!>{}<!>

    {
        if (true) true
    }();

    {
        if (true) true else false
    }();

    {
        if (true) {} else false
    }();


    {
        if (true) true else {}
    }()

    fun t(): Boolean {
        return <!TYPE_MISMATCH!><!INVALID_IF_AS_EXPRESSION!>if<!> (true) true<!>
    }

    return <!TYPE_MISMATCH!>if (true) true else {}<!>
}
