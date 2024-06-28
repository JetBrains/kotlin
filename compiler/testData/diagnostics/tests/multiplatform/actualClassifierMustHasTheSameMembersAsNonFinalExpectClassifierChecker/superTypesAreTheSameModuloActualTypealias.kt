// FIR_IDENTICAL
// MODULE: m1-common
// FILE: common.kt

expect open class Supertype()

expect open class Foo : Supertype {
    fun existingMethod()
    val existingParam: Int
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual typealias Supertype = SupertypeImpl

open class SupertypeImpl()

actual open class Foo : SupertypeImpl() {
    actual fun existingMethod() {}
    actual val existingParam: Int = 904
}
