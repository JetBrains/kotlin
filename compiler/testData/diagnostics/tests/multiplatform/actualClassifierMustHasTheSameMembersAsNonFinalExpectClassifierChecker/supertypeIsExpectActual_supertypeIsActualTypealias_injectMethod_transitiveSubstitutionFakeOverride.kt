// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// WITH_STDLIB
// MODULE: m1-common
// FILE: common.kt

expect open class Base<T>() {
    fun existingMethodInBase(param: T)
}

open class Transitive : Base<String>()

expect open class Foo : Transitive {
    fun existingMethod()
    val existingParam: Int
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual typealias Base<T> = BaseImpl<T>

open class BaseImpl<T> {
    fun existingMethodInBase(param: T) {}
    fun injected() {}
}

actual open class Foo : Transitive() {
    actual fun existingMethod() {}
    actual val existingParam: Int = 904
}
