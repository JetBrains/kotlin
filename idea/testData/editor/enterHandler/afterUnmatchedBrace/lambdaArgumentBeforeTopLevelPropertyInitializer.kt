// WITH_RUNTIME
val test = run {<caret>foo()

fun foo() = 42
//-----
// WITH_RUNTIME
val test = run {
    <caret>foo()
}

fun foo() = 42