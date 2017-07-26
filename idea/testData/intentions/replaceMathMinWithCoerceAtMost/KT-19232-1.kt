// WITH_RUNTIME

import java.lang.Math.min

fun foo() {
    Pair(<caret>min(1, 3), min(2, 4)).let { println(it) }
}