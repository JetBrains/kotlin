// "Rename parameter to match overridden method" "true"
abstract class A {
    abstract fun foo(arg : Int) : Int;
}

class B : A() {
    override fun foo(var arg: Int) : Int {
        arg += 5
        return arg
    }
}
