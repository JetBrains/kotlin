// MODULE: m1-common
// FILE: common.kt

open class Base() {
    <!INCOMPATIBLE_MATCHING{JVM}!>open fun overrideReturnType(): Any = ""<!>
    <!INCOMPATIBLE_MATCHING{JVM}!>open fun overrideModality1(): Any = ""<!>
    <!INCOMPATIBLE_MATCHING{JVM}!>open fun overrideModality2(): Any = ""<!>
    <!INCOMPATIBLE_MATCHING{JVM}!>protected open fun overrideVisibility(): Any = ""<!>
}

<!INCOMPATIBLE_MATCHING{JVM}!>expect open class Foo : Base {
    fun existingMethod()
    val existingParam: Int
}<!>

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual open class Foo : Base() {
    actual fun existingMethod() {}
    actual val existingParam: Int = 904

    fun injectedMethod() {}
    val injectedProperty: Int = 42
    override fun overrideReturnType(): String = ""
    final override fun overrideModality1(): Any = ""
    final override fun overrideModality2(): Any = ""
    public override fun overrideVisibility(): Any = ""
}
