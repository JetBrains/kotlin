// !DIAGNOSTICS: -UNUSED_PARAMETER

interface A
interface B : A
interface C : A

@Suppress(<!ERROR_SUPPRESSION!>"INVISIBLE_REFERENCE"<!>, "INVISIBLE_MEMBER", "HIDDEN")
fun <K> select(x: K, y: K): @kotlin.internal.Exact K = x

fun foo(a: Any) {}

fun test(b: B, c: C) {
    foo(
        select(b, c)
    )
}
