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

@OptIn(ExperimentalMultiplatform::class)
@AllowDifferentMembersInActual
actual open class Base<T> {
    actual fun existingMethodInBase(param: T) {}
    open fun injected(param: T): Any = ""
}

actual open class Foo : Transitive() {
    actual fun existingMethod() {}
    actual val existingParam: Int = 904

    override fun injected(param: String): String = "" // covariant override
}
