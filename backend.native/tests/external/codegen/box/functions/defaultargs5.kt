open abstract class B {
    abstract fun foo2(arg: Int = 239) : Int
}

class C : B() {
    override fun foo2(arg: Int) : Int = arg
}

fun box() : String {
    if(C().foo2() != 239) return "fail"
    if(C().foo2(10) != 10) return "fail"
    return "OK"
}
