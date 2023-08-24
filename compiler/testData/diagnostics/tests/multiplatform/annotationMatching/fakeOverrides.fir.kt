// MODULE: m1-common
// FILE: common.kt
annotation class Ann

abstract class A {
    @Ann
    open fun noAnnotationOnActual() {}
}

expect class FakeOverrideExpect : A

interface I {
    fun noAnnotationOnActual()
}

expect class FakeOverrideActual : I {
    @Ann
    override fun noAnnotationOnActual()
}

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
actual class FakeOverrideExpect : A() {
    override fun noAnnotationOnActual() {}
}

abstract class Intermediate : I {
    override fun noAnnotationOnActual() {}
}

<!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>actual class <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>FakeOverrideActual<!> : Intermediate(), I<!>
