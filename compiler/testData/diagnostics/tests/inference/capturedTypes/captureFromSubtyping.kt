fun <V, R, M : MutableMap<in R, out V>> mapKeysTo(destination: M): Inv3<R, V, M> {
    val foo = associateByTo(destination)

    return foo
}

fun < Y, Z, T : MutableMap<in Y, out Z>> associateByTo(<!UNUSED_PARAMETER!>destination<!>: T): Inv3<Y, Z, T> = TODO()

interface Inv3<A, B, C>
