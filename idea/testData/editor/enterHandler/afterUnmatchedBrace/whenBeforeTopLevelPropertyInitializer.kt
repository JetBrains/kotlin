val test = when {<caret>foo()

fun foo() = 42
//-----
val test = when {
    <caret>foo()
}

fun foo() = 42