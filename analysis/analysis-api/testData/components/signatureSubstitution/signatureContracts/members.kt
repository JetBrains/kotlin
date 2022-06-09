class X<T> {
    fun <A> foo(): T {}

    fun <A> A.foo1(t: T) {}

    fun <A> foo2(a: A) {}

    fun <A, B : Number> T.foo3(a: A): Map<B, List<A>> {}

    fun <A, B : Collection<A>, C : A> foo4(a: A, T): B {}

    val <A> A.bar1: T get() = 10

    val <A> A.bar2: Map<A, T> get() = mapOf()

}