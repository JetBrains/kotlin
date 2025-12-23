// ISSUE: KT-68538: The type of the outer if should not be Nothing because the inner ifs are not exhaustive.
// IGNORE_KLIB_BACKEND_ERRORS_WITH_CUSTOM_FIRST_STAGE: 2.0.0
// ^^^ KT-68538 was fixed in 2.0.10
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
