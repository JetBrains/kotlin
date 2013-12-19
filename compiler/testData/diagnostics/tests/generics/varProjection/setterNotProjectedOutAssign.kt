// !CHECK_TYPE
trait Tr<T> {
    var v: Tr<T>
}

fun test(t: Tr<*>) {
    t.v = t
    t.v checkType { it : _<Tr<*>> }
}