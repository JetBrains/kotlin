fun foo() {
    if ("abc" > "def" &&
            "qqq" < "bbb" &&
            "ddd" > "efg") {
        println("foo")
    }
}

fun foo() {
    if (
"abc" > "def" &&
"qqq" < "bbb" &&
"ddd" > "efg") {
        println("foo")
    }
}

// SET_TRUE: CONTINUATION_INDENT_IN_IF_CONDITIONS
