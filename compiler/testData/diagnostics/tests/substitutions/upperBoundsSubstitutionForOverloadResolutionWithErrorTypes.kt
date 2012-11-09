fun f1<T>(<!UNUSED_PARAMETER!>l<!>: <!UNRESOLVED_REFERENCE!>List1<!><T>): T {throw Exception()} // ERROR type here
fun f1<T>(<!UNUSED_PARAMETER!>l<!>: <!UNRESOLVED_REFERENCE!>List2<!><T>): T {throw Exception()} // ERROR type here
fun f1<T>(<!UNUSED_PARAMETER!>c<!>: Collection<T>): T{throw Exception()}

fun test<T>(l: List<T>) {
    <!CANNOT_COMPLETE_RESOLVE!>f1<!>(l)
}