// MODULE: m1-common
// FILE: common.kt

expect class A()

expect class B()

expect fun foo(test: String)

fun test() {
    A()
    B()
    foo("")
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

@Deprecated("", level = DeprecationLevel.HIDDEN)
actual class A

actual class B @Deprecated("", level = DeprecationLevel.HIDDEN) actual constructor(){}

@Deprecated("", level = DeprecationLevel.HIDDEN)
actual fun foo(test: String) {
}

fun main() {
    <!DEPRECATION_ERROR!>A<!>()
    <!DEPRECATION_ERROR!>B<!>()
    <!DEPRECATION_ERROR!>foo<!>("")
}
