fun <A> foo(): A {}

fun <A> A.foo1() {}

fun <A> foo2(a: A) {}

fun <A, B: Number> foo3(a: A): Map<B, List<A>> {}

fun <A, B: Collection<A>, C : A> foo4(a: A): B {}

val <A> A.bar1: Int get() = 10

val <A> A.bar2: List<A> get() = listOf(this)