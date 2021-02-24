fun test(foo: String) {
    <caret>if (foo == "1") {
        println("1")
    }

    if (foo == "a") {
        // some comments for "a"
        println("a");
    }
}

fun println(s: String) {}