// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_VARIABLE

fun example() {
    val a = if (true) true else false
    val b = <!INVALID_IF_AS_EXPRESSION!>if<!> (true) else false
    val c = <!INVALID_IF_AS_EXPRESSION!>if<!> (true) true
    val d = <!INVALID_IF_AS_EXPRESSION!>if<!> (true) true else;
    val e = if (true) {} else false
    val f = if (true) true else {}

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
        return <!INVALID_IF_AS_EXPRESSION!>if<!> (true) true
    }

    return <!RETURN_TYPE_MISMATCH!>if (true) true else {}<!>
}
