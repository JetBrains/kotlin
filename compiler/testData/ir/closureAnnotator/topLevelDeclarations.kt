fun Int.test1(x: Int) = this + x

class Test2(val x: Int) {
    fun test3() = x
    fun Int.test4() = this + x
}

val test5 = object {
    fun Int.test6(x: Int) = this + x
}