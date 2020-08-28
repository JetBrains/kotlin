// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER -TOPLEVEL_TYPEALIASES_ONLY

fun outer() {
    typealias Test1 = <!UNRESOLVED_REFERENCE!>Test1<!>
    typealias Test2 = <!UNRESOLVED_REFERENCE!>List<Test2><!>
    typealias Test3<T> = <!UNRESOLVED_REFERENCE!>List<Test3<T>><!>
}