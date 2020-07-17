interface A {
    val b: B
}

interface B
interface C : B {
    fun q(): Boolean
}

fun A.foo(): String = ""

fun main(a: A?) {
    val lb = a?.b
    if (lb !is C) return

    a.foo().length
}
