// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_VARIABLE

fun example() {
    val a = if (true) true else false
    val b = <!INVALID_IF_AS_EXPRESSION!>if<!> (true) else false
    val c = <!INVALID_IF_AS_EXPRESSION!>if<!> (true) true
    val d = <!INVALID_IF_AS_EXPRESSION!>if<!> (true) true else;
    val e = if (true) <!IMPLICIT_CAST_TO_ANY!>{}<!> else <!IMPLICIT_CAST_TO_ANY!>false<!>
    val f = if (true) <!IMPLICIT_CAST_TO_ANY!>true<!> else <!IMPLICIT_CAST_TO_ANY!>{}<!>

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
        return <!TYPE_MISMATCH!><!INVALID_IF_AS_EXPRESSION!>if<!> (true) true<!>
    }

    return <!NI;TYPE_MISMATCH!>if (true) <!OI;CONSTANT_EXPECTED_TYPE_MISMATCH!>true<!> else {}<!>
}