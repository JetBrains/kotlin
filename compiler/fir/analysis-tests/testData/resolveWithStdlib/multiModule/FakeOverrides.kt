// !LANGUAGE: +MultiPlatformProjects
// MODULE: m1-common
// FILE: common.kt

expect open class A<T>() {
    open fun foo(arg: T)
}
open class B : A<String>() {
    // Fake: override fun foo(arg: String) = super.foo(arg)
    // Fake (JVM only): override fun bar(arg: String): String = super.bar(arg)
}
open class C : B() {
    open fun <!VIRTUAL_MEMBER_HIDDEN!>bar<!>(arg: String): String = arg
    open fun baz(arg: CharSequence): String = arg.toString()
}

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt

actual open class A<T> {
    actual open fun foo(arg: T) {}
    open fun bar(arg: T): T = arg
    open fun baz(arg: T): T = arg
}
class D : C() {
    fun test() {
        foo("")
        bar("") // should be resolved to just C.bar
        baz("")
    }
}
