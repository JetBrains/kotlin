// FIR_IDENTICAL
// ISSUE: KT-57822

external interface Foo

fun bar() {
    foo()::class.simpleName
}

external fun foo(): Foo
