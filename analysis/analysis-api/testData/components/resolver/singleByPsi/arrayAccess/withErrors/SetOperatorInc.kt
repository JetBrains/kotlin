package test

class B(val n: Int) {
     operator fun inc() : B {return B(n + 1)}
}

fun test() {
    var a = B(1)
    a<caret>[2]++
}