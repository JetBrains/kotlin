// ISSUE: KT-61717
interface Foo<B : Foo<B>> {
    fun <T : B> bar(t: T)
}

class FooA : Foo<FooA> {
    override fun <T : FooA> bar(t: T) {}
}

class FooB : Foo<FooB> {
    override fun <T : FooB> bar(t: T) {}
}

fun testStar(foo1: Foo<*>, foo2: Foo<*>) {
    val x = foo1.bar(foo2)
}
