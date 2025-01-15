// LANGUAGE: +ContextParameters
// IGNORE_ERRORS
class A

interface First {
    <!CONTEXT_PARAMETERS_UNSUPPORTED!>context(a: <!DEBUG_INFO_MISSING_UNRESOLVED!>A<!>)<!>
    fun foo()

    <!CONTEXT_PARAMETERS_UNSUPPORTED!>context(a: <!DEBUG_INFO_MISSING_UNRESOLVED!>A<!>)<!>
    val b: String
}

interface Second {
    fun A.foo()
    val A.b: String
}

interface Third {
    fun foo(a: A)
}

interface IntersectionContextWithExtension : First, Second

interface IntersectionContextWithValue : First, Third