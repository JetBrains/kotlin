fun foo(f: (Int) -> String) {}

fun test() {
    foo <caret>{
        // comment1
        ""
        // comment2
    }
}