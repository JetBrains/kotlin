// WITH_RUNTIME
import java.util.*

fun test() {
    val mutableList = mutableListOf(1, 2)
    <caret>Collections.shuffle(mutableList, Random(1))
}
