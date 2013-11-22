// !DIAGNOSTICS: -UNREACHABLE_CODE -UNUSED_PARAMETER
// t is unused due to KT-4233
trait Tr<T> {
    var v: T
}

fun test(t: Tr<*>) {
    <!SETTER_PROJECTED_OUT!>t.v<!> = null!!
    val v = TypeOf(t.v)
    v: TypeOf<Any?>
}

class TypeOf<T>(t: T)