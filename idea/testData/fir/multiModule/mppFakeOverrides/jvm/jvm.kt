actual open class A<T> {
    actual open fun foo(arg: T) {}
    open fun bar(arg: T): T = arg
}
class D : C() {
    // Fake: override fun bar(arg: CharSequence): String = super.bar(arg)
    fun test() {
        foo("")
        bar("")
    }
}