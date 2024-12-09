// LANGUAGE: +ValhallaValueClasses
// IGNORE_BACKEND_K1: ANY
// ENABLE_JVM_PREVIEW
// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND: ANDROID
// IGNORE_DEXING
// CHECK_BYTECODE_LISTING

import kotlin.math.abs

value class OverridesHashCode(val x: Int) {
    override fun hashCode(): Int = -x
}

value class OverridesEquality(val x: Int) {
    override fun equals(other: Any?): Boolean = other is OverridesEquality && abs(this.x) == abs(other.x)
    override fun hashCode(): Int = abs(x)
}

value class FakeBoxing(val x: Int) {
    fun unbox() = x * 2
    fun `unbox-impl`() = x * 3
    companion object {
        @JvmStatic
        fun box(x: Int) = OverridesEquality(x * 4)
        @JvmStatic
        fun `box-impl`(x: Int) = OverridesEquality(x * 5)
    }
}

fun box(): String {
    // Test OverridesHashCode
    val hashCodeInstance = OverridesHashCode(10)
    require(hashCodeInstance.hashCode() == -10) { hashCodeInstance.hashCode().toString() }

    // Test OverridesEquality
    val equalityInstance1 = OverridesEquality(20)
    val equalityInstance2 = OverridesEquality(-20)
    require(equalityInstance1 == equalityInstance2) { "$equalityInstance1 != $equalityInstance2" }

    // Test FakeBoxing methods
    val fakeBoxingInstance = FakeBoxing(30)
    require(fakeBoxingInstance.unbox() == 60) { "unbox(): ${fakeBoxingInstance.unbox()}" }
    require(fakeBoxingInstance.`unbox-impl`() == 90) { "`unbox-impl`(): ${fakeBoxingInstance.`unbox-impl`()}" }

    // Test FakeBoxing companion object methods
    val boxedInstance = FakeBoxing.box(5)
    require(boxedInstance == OverridesEquality(20)) { boxedInstance.toString() }
    val boxedImplInstance = FakeBoxing.`box-impl`(5)
    require(boxedImplInstance == OverridesEquality(25)) { boxedImplInstance.toString() }

    return "OK"
}
