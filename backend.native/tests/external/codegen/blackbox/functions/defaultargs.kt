open abstract class B {
    fun foo(arg: Int = 239 + 1) : Int = arg
}

class C() : B() {
}

fun box() : String {
    if(C().foo(10) != 10) return "fail"
    if(C().foo() != 240) return "fail"
    return "OK"
}
