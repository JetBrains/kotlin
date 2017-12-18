// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_EXPRESSION

import java.util.HashSet

fun test123() {
    val g: (Int) -> Unit = if (true) {
        val set = HashSet<Int>()
        fun (i: Int) {
            set.add(i)
        }
    }
    else {
        { it -> it }
    }
}