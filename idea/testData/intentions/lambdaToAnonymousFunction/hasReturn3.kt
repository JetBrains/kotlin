fun foo(f: (Int) -> String) {}

fun test() {
    foo {<caret>
        if (it == 1) {
            return@foo "1"
        } else if (it == 2) {
            return@foo "2"
        } else {
            return@foo "$it"
        }
    }
}