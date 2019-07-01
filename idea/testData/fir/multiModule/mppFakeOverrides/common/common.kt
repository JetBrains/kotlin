expect open class A<T>() {
    open fun foo(arg: T)
}
open class B : A<String>() {
    // Fake: override fun foo(arg: String) = super.foo(arg)
    // Fake (JVM only): override fun bar(arg: String): String = super.bar(arg)
}
open class C : B() {
    open fun bar(arg: String): String = arg
    open fun baz(arg: CharSequence): String = arg.toString()
}