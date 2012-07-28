abstract class B() {
    abstract fun foo2(arg: Int = 239) : Int
}

class C() : B() {
    override fun foo2(arg: Int) : Int = arg
}

fun invokeIt() {
    C().foo2()
}
