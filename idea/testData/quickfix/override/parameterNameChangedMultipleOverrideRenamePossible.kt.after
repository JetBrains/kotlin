// "Rename parameter to match overridden method" "true"
abstract class A {
    abstract fun foo(arg : Int) : Int;
}

trait X {
    fun foo(arg : Int) : Int;
}

class B : A(), X {
    override fun foo(arg: Int) : Int {
        val x = arg + arg
        return arg
    }
}
