// !LANGUAGE: +MultiPlatformProjects
// !DIAGNOSTICS: -UNSUPPORTED
// MODULE: m1-common
// FILE: common.kt

header class Foo {
    constructor(p: Any)

    fun f1(s: String): Int

    fun f2(s: List<String>?): MutableMap<Boolean?, Foo>

    fun <T : Set<Number>> f3(t: T): T?
}

// MODULE: m2-js(m1-common)
// FILE: js.kt

// TODO: do not suppress UNSUPPORTED once JS files in multi-platform tests are analyzed with JS analyzer facade

impl class Foo {
    impl constructor(p: dynamic) {}

    impl fun f1(s: dynamic): dynamic = null!!

    impl fun f2(s: dynamic): MutableMap<Boolean?, Foo> = null!!

    impl fun <T : Set<Number>> f3(t: T): dynamic = null!!
}
