class A(val n: Int)

fun test() {
    <selection>fun foo<T: A>(t: T): Int {
        fun A.a(n: Int): Int = this.n + n
        fun A.b(n: Int): Int = this.n - n

        return t.n + A(1).a(2) - A(2).b(1)
    }</selection>

    fun foo<U: A>(u: U): Int {
        fun A.x(m: Int): Int = n + m
        fun A.y(n: Int): Int = this.n - n

        return u.n + A(1).x(2) - A(2).y(1)
    }

    fun foo<V: A>(v: V): Int {
        fun A.a(n: Int): Int = this.n + n
        fun A.b(n: Int): Int = this.n + n

        return v.n + A(1).a(2) - A(2).b(1)
    }
}