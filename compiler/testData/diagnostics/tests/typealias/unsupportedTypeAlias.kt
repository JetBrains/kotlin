// !LANGUAGE: -TypeAliases

class C

<!UNSUPPORTED_TYPEALIAS!>typealias<!> S = String
<!UNSUPPORTED_TYPEALIAS!>typealias<!> L<T> = List<T>
<!UNSUPPORTED_TYPEALIAS!>typealias<!> CA = C

val test1: <!UNRESOLVED_REFERENCE!>S<!> = ""

fun test2(x: <!UNRESOLVED_REFERENCE!>L<!><<!UNRESOLVED_REFERENCE!>S<!>>) = <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>x<!>

class Test3 : <!UNRESOLVED_REFERENCE!>CA<!>()