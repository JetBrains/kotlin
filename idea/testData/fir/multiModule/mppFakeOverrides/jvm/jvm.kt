actual open class A<T> {
    actual open fun foo(arg: T) {}
    open fun bar(arg: T): T = arg
    open fun baz(arg: T): T = arg
}
class D : C() {
    fun test() {
        foo("")
        bar("") // should be resolved to just C.bar
        baz("") // ambiguity (we have C.bar with CharSequence argument and fake override of A.bar with String argument)
    }
}