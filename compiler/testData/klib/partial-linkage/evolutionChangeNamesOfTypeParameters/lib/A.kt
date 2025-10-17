interface B<T> {
    var few: T
}

class A<X, Y, Z, W>(z: Z, w: W): B<W> {
    var bar: Z = z
    override var few: W = w
    fun foo(x: X): Y {
        return x as Y
    }
}

fun <U, V> qux(u: U): V {
    return u as V
}

