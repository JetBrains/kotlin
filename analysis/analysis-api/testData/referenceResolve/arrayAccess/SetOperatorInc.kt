package test

class B(val n: Int) {
    operator fun set(i: Int, a: B) {}
    operator fun get(i: Int) : B { return B(i) }
    operator fun inc() : B {return B(n + 1)}
}

fun test() {
    var a = B(1)
    a<caret>[2]++
}