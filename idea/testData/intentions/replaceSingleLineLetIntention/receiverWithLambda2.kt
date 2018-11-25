// WITH_RUNTIME
// IS_APPLICABLE: false
// This should be reported. However, in order to avoid too complicate logic, the intention ignore this case.

import java.util.*

fun baz2(foo: List<String>) {
    foo.let<caret> { it.binarySearch("", Comparator<kotlin.String> { o1, o2 -> 0 }) }
}