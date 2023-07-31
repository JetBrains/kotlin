// RENDER_DIAGNOSTICS_FULL_TEXT
// MODULE: m1-common
// FILE: common.kt
annotation class Ann

@Ann
expect class OnClass

expect class OnMember {
    @Ann
    fun onMember()
}

@Ann
expect class ViaTypealias {
    @Ann
    fun foo()
}

annotation class WithArg(val s: String)

@WithArg("str")
expect fun withDifferentArg()

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
actual class <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>OnClass<!>

actual class OnMember {
    actual fun <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>onMember<!>() {}
}

class ViaTypealiasImpl {
    fun foo() {}
}

actual typealias <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>ViaTypealias<!> = ViaTypealiasImpl

@WithArg("other str")
actual fun <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>withDifferentArg<!>() {}
