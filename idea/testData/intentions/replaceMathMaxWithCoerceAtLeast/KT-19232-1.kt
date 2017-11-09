// WITH_RUNTIME

import java.lang.Math.max

fun foo() {
    Pair(<caret>max(1, 3), max(2, 4)).let { println(it) }
}