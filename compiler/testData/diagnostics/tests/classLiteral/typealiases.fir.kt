typealias TString = String
fun f1() = TString::class

typealias TNullableString = String?
fun f2() = TNullableString::class

typealias TNullableTString = TString?
typealias TTNullableTString = TNullableTString
fun f3() = TTNullableTString::class

inline fun <reified T> f4(b: Boolean): Any {
    <!TOPLEVEL_TYPEALIASES_ONLY!>typealias X = <!TYPEALIAS_SHOULD_EXPAND_TO_CLASS!>T<!><!>
    <!TOPLEVEL_TYPEALIASES_ONLY!>typealias Y = <!TYPEALIAS_SHOULD_EXPAND_TO_CLASS!>T?<!><!>
    return if (b) <!UNRESOLVED_REFERENCE!>X<!>::class else <!UNRESOLVED_REFERENCE!>Y<!>::class
}
