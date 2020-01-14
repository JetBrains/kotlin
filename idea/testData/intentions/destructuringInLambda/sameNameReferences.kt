data class D(val v1: Int, val v2: Int)

fun foo(f: (D) -> Int) {
}

fun bar() {}

fun test() {
    foo { <caret>bar ->
        bar()
        bar.v1 + bar.v2
    }
}