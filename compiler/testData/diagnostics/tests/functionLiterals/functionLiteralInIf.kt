// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_EXPRESSION

import java.util.HashSet

fun test123() {
    val g: (Int) -> Unit = if (true) {
        val set = HashSet<Int>();
        { i ->
            set.add(i)
        }
    }
    else {
        { it -> it }
    }
}