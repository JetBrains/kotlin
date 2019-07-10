// WITH_RUNTIME
import java.util.Arrays

fun test() {
    val a = arrayOf(1, 2, 3)
    val b = arrayOf(1, 2, 3)
    // comment
    Arrays.<caret>equals(a, b) //comment2
}