// FILE: 1.kt

class Inline() {

    inline fun foo(closure1 : (l: Int) -> String, param: Int, closure2: String.() -> Int) : Int {
        return closure1(param).closure2()
    }
}

// FILE: 2.kt

fun test1(): Int {
    val inlineX = Inline()
    return inlineX.foo({ z: Int -> "" + z}, 25, fun String.(): Int = this.length)
}

fun box(): String {
    if (test1() != 2) return "test1: ${test1()}"

    return "OK"
}
