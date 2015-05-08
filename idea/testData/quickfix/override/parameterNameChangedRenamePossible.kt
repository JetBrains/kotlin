// "Rename parameter to match overridden method" "true"
abstract class A {
    abstract fun foo(arg : Int) : Int;
}

class B : A() {
    override fun foo(agr<caret> : Int) : Int {
        var x = agr + agr
        return agr
    }
}
