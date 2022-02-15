// !DIAGNOSTICS: -UNSUPPORTED
// MODULE: m1-common
// FILE: common.kt

expect class Foo {
    constructor(p: Any)

    fun f1(s: String): Int

    fun f2(s: List<String>?): MutableMap<Boolean?, Foo>

    fun <T : Set<Number>> f3(t: T): T?
}

// MODULE: m2-js()()(m1-common)
// FILE: js.kt

// TODO: do not suppress UNSUPPORTED once JS files in multi-platform tests are analyzed with JS analyzer facade

actual class <!NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS!>Foo<!> {
    <!ACTUAL_WITHOUT_EXPECT!>actual constructor(p: dynamic) {}<!>

    <!ACTUAL_WITHOUT_EXPECT!>actual fun f1(s: dynamic): dynamic = null!!<!>

    <!ACTUAL_WITHOUT_EXPECT!>actual fun f2(s: dynamic): MutableMap<Boolean?, Foo> = null!!<!>

    <!ACTUAL_WITHOUT_EXPECT!>actual fun <T : Set<Number>> f3(t: T): dynamic = null!!<!>
}
