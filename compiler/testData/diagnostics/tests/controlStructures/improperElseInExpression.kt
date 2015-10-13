// !DIAGNOSTICS: -UNUSED_VARIABLE

fun example() {
    val a = if (true) true else false
    val b = <!INVALID_IF_AS_EXPRESSION!>if (true) else false<!>
    val c = <!INVALID_IF_AS_EXPRESSION!>if (true) true<!>
    val d = <!INVALID_IF_AS_EXPRESSION!>if (true) true else<!>;
    val e = <!IMPLICIT_CAST_TO_UNIT_OR_ANY!>if (true) {} else false<!>
    val f = <!IMPLICIT_CAST_TO_UNIT_OR_ANY!>if (true) true else {}<!>

    {
        if (true) <!UNUSED_EXPRESSION!>true<!>
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
        return <!TYPE_MISMATCH, INVALID_IF_AS_EXPRESSION!>if (true) true<!>
    }

    return if (true) <!CONSTANT_EXPECTED_TYPE_MISMATCH!>true<!> else {}
}