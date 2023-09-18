// ISSUE: KT-61933

data class Bar(
    val foo: Foo<suspend () -> Unit>
)

data class Foo<out TCallback : Any>(
    val state: TCallback?,
)

fun usage(b: Boolean) {
    Bar(
        foo = <!ARGUMENT_TYPE_MISMATCH!>Foo(
            state = if (b) { -> } else null,
        )<!>
    )
}
