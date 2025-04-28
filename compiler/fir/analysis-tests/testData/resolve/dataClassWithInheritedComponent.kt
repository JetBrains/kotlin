// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-76898

interface Foo {
    operator fun component1() = 42
}

data class Bar(val bar: String): Foo
