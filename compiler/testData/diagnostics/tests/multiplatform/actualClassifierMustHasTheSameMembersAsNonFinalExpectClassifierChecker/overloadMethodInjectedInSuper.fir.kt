// MODULE: m1-common
// FILE: common.kt

expect open class Base

expect open class Foo : Base {
    fun foo(param: Int)
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual open class Base {
    open fun foo(): Any = ""
}

actual open class Foo : Base() {
    override fun foo(): String = ""

    actual fun foo(param: Int) {}
}
