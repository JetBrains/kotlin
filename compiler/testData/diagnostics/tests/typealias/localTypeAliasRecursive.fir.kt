// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER -TOPLEVEL_TYPEALIASES_ONLY

fun outer() {
    typealias Test1 = Test1
    typealias Test2 = List<Test2>
    typealias Test3<T> = List<Test3<T>>
}