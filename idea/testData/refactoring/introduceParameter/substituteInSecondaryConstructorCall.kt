// WITH_DEFAULT_VALUE: false
open class A {
    constructor(): this(1)

    constructor(a: Int) {
        val t = (<selection>a + 1</selection>) / 2
    }
}

class B: A {
    constructor(n: Int): super(n + 1)
}

class C: A(1) {

}

fun test() = A(1)