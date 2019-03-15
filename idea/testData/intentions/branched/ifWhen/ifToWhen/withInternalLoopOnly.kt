// WITH_RUNTIME

fun testIf(x: Any) {
    <caret>if (x is String) {
        println(x)
        for (c in x) {
            if (c == ' ')
                break // do not change
        }
    }
    else {
        println(x)
    }
}