package Hello

open class Base<T>
class StringBase : Base<String>()

class Client<T, X: Base<T>>(<!UNUSED_PARAMETER!>x<!>: X)

fun test() {
    val c = Client(StringBase()) // Type inference fails here for T.
    val <!UNUSED_VARIABLE!>i<!> : Int = <!TYPE_MISMATCH!>c<!>
}