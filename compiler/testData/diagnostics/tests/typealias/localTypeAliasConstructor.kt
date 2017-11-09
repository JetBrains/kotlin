// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER -TOPLEVEL_TYPEALIASES_ONLY

class Cell<TC>(val x: TC)

fun <T> id(x: T): T {
    typealias C = Cell<T>
    class Local(val cell: C)
    val cx = C(x)
    val c: C = Local(cx).cell
    return c.x
}
