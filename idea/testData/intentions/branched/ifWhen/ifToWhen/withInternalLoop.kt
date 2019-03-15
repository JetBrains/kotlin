// WITH_RUNTIME

fun testIf(xs: List<Any>) {
    for (x in xs) {
        <caret>if (x is String) {
            for (c in x) {
                continue // do not change
            }
        }
        else if (x is Int) {
            break
        }
        else {
            println(x)
        }
    }
}