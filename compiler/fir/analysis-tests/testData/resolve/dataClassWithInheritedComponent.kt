// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-76898

interface Foo {
    operator fun component1() = 42
}

<!DATA_CLASS_OVERRIDE_CONFLICT!>data<!> class Bar(val bar: String): Foo
