// !CHECK_TYPE

trait B<T>
trait G<T>: B<T>

fun f(p: B<<!UNRESOLVED_REFERENCE!>Foo<!>>): Any {
    val v = <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>p<!> as G
    return checkSubtype<G<*>>(<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>v<!>)
}