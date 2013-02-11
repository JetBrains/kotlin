// "Rename parameter to match overridden method" "true"
abstract class A {
    abstract fun foo(arg : Int) : Int;
}

class B : A() {
    override fun foo(arg: Int) : Int {
        var x = arg + arg
        return arg
    }
}
