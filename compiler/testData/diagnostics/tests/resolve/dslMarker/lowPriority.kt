// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_PARAMETER, -ERROR_SUPPRESSION
@DslMarker
annotation class Ann

@Ann
class A {
    @Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
    @kotlin.internal.LowPriorityInOverloadResolution
    fun a() = 1
}

@Ann
class B {
    fun b() = 2
}

fun foo(x: A.() -> Unit) {}
fun bar(x: B.() -> Unit) {}

fun test() {
    foo {
        a()
        bar {
            <!DSL_SCOPE_VIOLATION!>a<!>()
        }
    }
}
