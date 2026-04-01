
fun foo(action: (Int) -> Unit) {}

fun usage() {
    foo { ar<caret>g ->

    }
}
