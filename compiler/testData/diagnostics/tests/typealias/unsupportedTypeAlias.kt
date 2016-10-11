// !LANGUAGE: -TypeAliases

class C

<!UNSUPPORTED_FEATURE!>typealias<!> S = String
<!UNSUPPORTED_FEATURE!>typealias<!> L<T> = List<T>
<!UNSUPPORTED_FEATURE!>typealias<!> CA = C
<!UNSUPPORTED_FEATURE!>typealias<!> Unused = Any

val test1: <!UNSUPPORTED_FEATURE!>S<!> = ""

fun test2(x: <!UNSUPPORTED_FEATURE!>L<<!UNSUPPORTED_FEATURE!>S<!>><!>) = <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>x<!>

class Test3 : <!UNSUPPORTED_FEATURE!>CA<!>()