// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// LANGUAGE: +MangleCallsToJavaMethodsWithValueClasses


// FILE: KotlinParent.kt
open class KotlinParent {
    fun foo(type: InlineType) = 42

    @JvmInline
    value class InlineType(val id: Int)
}

// FILE: JavaChild.java
class JavaChild extends KotlinParent {}


// FILE: box.kt
fun box(): String {
    if (JavaChild().foo(KotlinParent.InlineType(1)) != 42) return "Fail"
    return "OK"
}