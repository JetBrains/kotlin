// WITH_STDLIB
// MODULE: m1-common
// FILE: common.kt

expect open class Base {
    fun existingMethodInBase()
}

expect open class Foo : Base {
    fun existingMethod()
    val existingParam: Int
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

@OptIn(ExperimentalMultiplatform::class)
@AllowDifferentMembersInActual
actual open class Base {
    actual fun existingMethodInBase() {}
    open fun injected(): Any = ""
}

actual open class Foo : Base() {
    actual fun existingMethod() {}
    actual val existingParam: Int = 904

    override fun injected(): String = "" // covariant override
}
