trait Tr<T> {
    var v: Tr<T>
}

fun test(t: Tr<*>) {
    t.v = t
    val v = TypeOf(t.v)
    v: TypeOf<Tr<*>>
}

class TypeOf<T>(t: T)