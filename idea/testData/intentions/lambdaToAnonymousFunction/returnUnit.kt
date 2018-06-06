fun unit(f: (Int) -> Unit) {}

fun foo(i: Int) {}

fun test() {
    unit {<caret>
        foo(it)
        foo(it)
    }
}