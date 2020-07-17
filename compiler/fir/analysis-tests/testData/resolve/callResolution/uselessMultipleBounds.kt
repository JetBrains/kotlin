// ISSUE: KT-39032

interface A {
    fun foo()
}

interface B : A {
    override fun foo()
}

fun <E> bar(e: E) where E : A, E : B {
    e.foo()
}
