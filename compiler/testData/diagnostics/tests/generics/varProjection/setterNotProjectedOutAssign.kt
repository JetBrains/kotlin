// !CHECK_TYPE
interface Tr<T> {
    var v: Tr<T>
}

fun test(t: Tr<*>) {
    t.v = <!TYPE_MISMATCH!>t<!>
    t.v checkType { _<Tr<*>>() }
}