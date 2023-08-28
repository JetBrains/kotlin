// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER -TOPLEVEL_TYPEALIASES_ONLY

fun outer() {
    typealias Test1 = <!UNRESOLVED_REFERENCE!>Test1<!>
    typealias Test2 = List<<!UNRESOLVED_REFERENCE!>Test2<!>>
    typealias Test3<T> = List<<!UNRESOLVED_REFERENCE!>Test3<!><T>>
}
