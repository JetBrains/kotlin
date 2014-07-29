class A(val n: Int)

fun test() {
    <selection>fun foo<T: A>(t: T): Int {
        fun a(p: Int): Int = p + 1
        fun b(q: Int): Int = q - 1

        return t.n + a(1) - b(2)
    }</selection>

    fun foo<U: A>(u: U): Int {
        fun x(a: Int): Int = a + 1
        fun y(a: Int): Int = a - 1

        return u.n + x(1) - y(2)
    }

    fun foo<V: A>(v: V): Int {
        fun a(p: Int): Int = p + 1
        fun b(p: Int): Int = p + 1

        return v.n + a(1) - b(2)
    }

    fun a(p: Int): Int = p + 1
    fun b(q: Int): Int = q - 1

    fun foo<W: A>(w: W): Int {
        return w.n + a(1) - b(2)
    }
}