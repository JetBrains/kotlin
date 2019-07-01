// "Change function signature to 'fun <T : Number> f(a: T)'" "true"
open class A {
    open fun <T : Number> f(a: T) {}
}

class B : A() {
    <caret>override fun <E : Enum<E>> f(a: E) {}
}
