// WITH_RUNTIME
import java.util.Arrays.hashCode

fun test() {
    val a = arrayOf(1)
    val hash = <caret>hashCode(a)
}