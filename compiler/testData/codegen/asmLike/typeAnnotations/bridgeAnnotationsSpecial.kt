// EMIT_JVM_TYPE_ANNOTATIONS
// JVM_DEFAULT_MODE: no-compatibility
// JVM_TARGET: 1.8
// RENDER_ANNOTATIONS
// WITH_STDLIB
// LANGUAGE: +JvmEnhancedBridges

@file:OptIn(ExperimentalStdlibApi::class)

@Target(AnnotationTarget.FUNCTION)
annotation class OverriddenAnno

@Target(AnnotationTarget.FUNCTION)
annotation class TargetAnno


interface A<T1, T2> {
    @OverriddenAnno
    @Throws(NullPointerException::class)
    fun testThrows(t: T1)

    fun testSynchronized(t: T1)

    fun testExposeBoxed(t: T2)

    fun testOverloads(t: T1?)

    fun testJvmName(t: T1)
}

open class B {
    @TargetAnno
    @Throws(Exception::class)
    fun testThrows(t: String) {}

    @Synchronized
    fun testSynchronized(t: String) {}

    @JvmExposeBoxed("testExposeBoxedBoxed")
    fun testExposeBoxed(t: UInt) {}

    @JvmOverloads
    open fun testOverloads(t: String? = null) {}

    @JvmName("customName")
    fun testJvmName(t: String) {}
}

class J: B(), A<String, UInt> {
}