fun baz(name: String, f: (Int) -> String) {}

fun test() {
    baz(name = "", f = <caret>{ "$it" })
}