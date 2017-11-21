// RUNTIME_WITH_FULL_JDK
import java.util.*

fun test() {
    val mutableList = mutableListOf(1, 2)
    <caret>Collections.shuffle(mutableList, Random(1))
}
