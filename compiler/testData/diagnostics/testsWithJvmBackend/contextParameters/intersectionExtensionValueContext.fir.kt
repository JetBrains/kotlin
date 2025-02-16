// LANGUAGE: +ContextParameters
// IGNORE_ERRORS
class A

interface First {
    context(a: A)
    fun foo()

    context(a: A)
    val b: String
}

interface Second {
    fun A.foo()
    val A.b: String
}

interface Third {
    fun foo(a: A)
}

<!CONFLICTING_INHERITED_JVM_DECLARATIONS, CONFLICTING_INHERITED_JVM_DECLARATIONS!>interface IntersectionContextWithExtension : First, Second<!>

<!CONFLICTING_INHERITED_JVM_DECLARATIONS!>interface IntersectionContextWithValue : First, Third<!>
