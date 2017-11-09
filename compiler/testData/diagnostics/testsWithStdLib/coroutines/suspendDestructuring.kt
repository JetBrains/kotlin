// SKIP_TXT
class A {
    suspend operator fun component1(): String = "K"
}

fun foo(<!UNUSED_PARAMETER!>c<!>: suspend (A) -> Unit) {}

fun bar() {
    foo {
        (x) ->
        x.length
    }
}
