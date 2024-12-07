// ISSUE: KT-68538: The type of the outer if should not be Nothing because the inner ifs are not exhaustive.
// WITH_STDLIB
import kotlin.test.*

fun box(): String {
    val result = buildList {
        var index = 0
        while (index <= 10) {
            if (index > 4) {
                if (index == 5) break
            } else {
                if (index == 3) break
            }
            add(index)
            index++
        }
    }
    assertEquals(listOf(0, 1, 2), result)
    return "OK"
}
