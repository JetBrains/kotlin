// ISSUE: KT-9152

abstract class A {
    open fun <T> f(x: T): T {
        abstract class B : A() {
            abstract override fun <S> f(x: T): S
        }
        null!!
    }
}
