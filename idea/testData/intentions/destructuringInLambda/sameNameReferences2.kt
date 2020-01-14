data class D(val v1: Int, val v2: Int)

fun foo(f: (D) -> Int) {
}

fun it() {}

fun test() {
    foo {<caret>
        it()
        it.v1 + it.v2
    }
}