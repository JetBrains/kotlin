internal class My

class Your

// Both arguments should be exposed
fun foo(<!EXPOSED_PARAMETER_TYPE!>my: My<!>, <!EXPOSED_PARAMETER_TYPE!>f: (My) -> Unit<!>) = f(my)

// Ok
fun bar(your: Your, f: (Your) -> Unit) = f(your)

// Exposed, returns My
fun <!EXPOSED_FUNCTION_RETURN_TYPE!>gav<!>(<!EXPOSED_PARAMETER_TYPE!>f: () -> My<!>) = f()