// "Rename parameter to match overridden method" "false"
abstract class A {
    abstract fun foo(arg : Int) : Int;
}

trait X {
    fun foo(agr: Int) : Int;
}

class B : A(), X {
    override fun foo(var arg<caret>: Int) : Int {
        arg += 5
        return arg
    }
}
