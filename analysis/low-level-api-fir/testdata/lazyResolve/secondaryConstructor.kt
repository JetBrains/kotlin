fun resolveMe() {
    receive(A(42))
}

fun receive(value: A){}

class A {
    constructor(x: Int) {
        val a = x
    }
}