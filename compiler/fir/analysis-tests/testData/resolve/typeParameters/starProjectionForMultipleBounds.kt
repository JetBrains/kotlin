// ISSUE: KT-7389

class Inv<T> (val value: T) where T: A, T: B

interface A {
    fun doA()
}

interface B {
    fun doB()
}

fun process(c: Inv<*>) {
    c.value.doA()
    c.value.doB()
}
