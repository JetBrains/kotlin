// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER -TOPLEVEL_TYPEALIASES_ONLY

fun outer() {
    typealias Test1 = <!OTHER_ERROR!>Test1<!>
    typealias Test2 = <!OTHER_ERROR!>List<Test2><!>
    typealias Test3<T> = <!OTHER_ERROR!>List<Test3<T>><!>
}