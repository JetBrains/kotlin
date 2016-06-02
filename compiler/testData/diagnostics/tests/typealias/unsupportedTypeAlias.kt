// !LANGUAGE: -TypeAliases

class C

<!UNSUPPORTED_TYPEALIAS!>typealias<!> S = String
<!UNSUPPORTED_TYPEALIAS!>typealias<!> L<T> = List<T>
<!UNSUPPORTED_TYPEALIAS!>typealias<!> CA = C

val test1: <!UNSUPPORTED_TYPEALIAS!>S<!> = ""

fun test2(x: <!UNSUPPORTED_TYPEALIAS!>L<<!UNSUPPORTED_TYPEALIAS!>S<!>><!>) = <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>x<!>

class Test3 : <!UNSUPPORTED_TYPEALIAS!>CA<!>()