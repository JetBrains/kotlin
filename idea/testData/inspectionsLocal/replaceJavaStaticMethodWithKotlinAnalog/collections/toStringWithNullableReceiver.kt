// WITH_RUNTIME
// PROBLEM: none
import java.util.Arrays

fun test() {
    val a: Array<*>? = arrayOf(1)
    val str = Arrays.<caret>toString(a)
}