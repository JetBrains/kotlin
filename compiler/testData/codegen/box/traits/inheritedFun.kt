//KT-2206
interface A {
    fun f():Int = 239
}

class B() : A

fun box() : String {
    return if (B().f() == 239) "OK" else "fail"
}
