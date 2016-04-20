// WITH_RUNTIME
// IS_APPLICABLE: false
import java.util.*

fun foo(list: List<String>): String? {
    val random = Random()
    for (s in list) {
        if (random.nextBoolean()) {
            return s
        }
    }
    return null
}