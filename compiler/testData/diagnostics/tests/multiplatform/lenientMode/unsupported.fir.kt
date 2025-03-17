// RUN_PIPELINE_TILL: FIR2IR
// LANGUAGE: +MultiPlatformProjects
// LENIENT_MODE

// MODULE: common
// FILE: common.kt
<!NO_ACTUAL_FOR_EXPECT{JVM}!>expect<!> enum class E {
    Foo, Bar,
}

<!NO_ACTUAL_FOR_EXPECT{JVM}!>expect<!> annotation class A

<!NO_ACTUAL_FOR_EXPECT{JVM}!>expect<!> value class V(val s: String)

open class C1(s: String)

<!NO_ACTUAL_FOR_EXPECT{JVM}!>expect<!> class C2 : C1

// MODULE: jvm()()(common)
// FILE: jvm.kt
fun main() {}