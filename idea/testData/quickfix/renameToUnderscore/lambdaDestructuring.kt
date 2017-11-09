// "Rename to _" "true"

data class A(val x: String, val y: Int)

fun foo(block: (A) -> Unit) {
    block(A("", 1))
}

fun bar() {
    foo { (x<caret>, y: Int) ->
        y.hashCode()
    }
}
