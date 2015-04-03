class A {
    constructor(a: Int) {
        val b = (<selection>a + 1</selection>) * 2
        val t = a + b
    }
}

fun test() {
    A(1)
}