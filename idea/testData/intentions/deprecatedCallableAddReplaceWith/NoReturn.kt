// IS_APPLICABLE: false
// ERROR: A 'return' expression required in a function with a block body ('{...}')

<caret>@deprecated("")
fun foo(): String {
    bar()
}

fun bar(): String = ""