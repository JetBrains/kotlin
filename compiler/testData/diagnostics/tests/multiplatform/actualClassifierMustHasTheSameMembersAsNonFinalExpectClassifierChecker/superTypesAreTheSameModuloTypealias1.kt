// FIR_IDENTICAL
// MODULE: m1-common
// FILE: common.kt

open class Supertype()
typealias SupertypeAlias = Supertype

expect open class Foo : Supertype {
    fun existingMethod()
    val existingParam: Int
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual open class Foo : SupertypeAlias() {
    actual fun existingMethod() {}
    actual val existingParam: Int = 904
}
