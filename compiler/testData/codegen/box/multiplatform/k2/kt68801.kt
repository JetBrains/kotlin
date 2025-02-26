// LANGUAGE: +MultiPlatformProjects
// ISSUE: KT-68801
// IGNORE_NATIVE: compatibilityTestMode=BACKWARD_2_0
// ^^^ Compiler v2.0.0: No override for FUN FAKE_OVERRIDE name:foo visibility:public modality:FINAL <> ($this:<root>.Base) returnType:kotlin.String [fake_override] in CLASS CLASS name:B modality:FINAL visibility:public superTypes:[<root>.A]

// MODULE: common
// FILE: common.kt
open expect class A() {
    fun foo(): String
}

expect class B() : A

fun test() = B().foo()

// MODULE: platform()()(common)
// FILE: platform.kt
open class Base {
    fun foo() = "OK"
}

actual open class A : Base()

actual class B : A()

fun box() : String {
    return test()
}
