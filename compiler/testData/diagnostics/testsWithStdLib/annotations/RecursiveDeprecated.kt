// KT-6367 / EA-75125: 
// Recursion detected on input: AssertionError in IDE and ClassCastException in the compiler

@Deprecated(<!TYPE_MISMATCH!><!DEPRECATION!>bar<!>()<!>)
fun bar() = null

<!DEPRECATED_JAVA_ANNOTATION!>@java.lang.Deprecated(<!TOO_MANY_ARGUMENTS!><!DEPRECATION!>foo<!>()<!>)<!>
fun foo() = "x"

