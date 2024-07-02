// ISSUE: KT-69463
// LANGUAGE: +MultiPlatformProjects
// IGNORE_NON_REVERSED_RESOLVE

// MODULE: common
// FILE: common.kt
expect interface <!NO_ACTUAL_FOR_EXPECT!>Foo<!> {
    fun test(a: Int = 7)
}

// MODULE: platform()()(common)
// FILE: platform.kt
actual interface Foo {
    actual fun test(a: Int)
}

open class FooFoo(val a: Foo): Foo by a

class Final(f:Foo): FooFoo(f) {
    override fun test(a: Int) {
        super.<!SUPER_CALL_WITH_DEFAULT_PARAMETERS!>test<!>()
    }
}
