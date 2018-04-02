fun foo(f: (Int) -> String) {}

fun test() {
    foo <caret>{
        val b = it == 1
        if (b) "1" else "2"
    }
}