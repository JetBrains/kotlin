import java.util.ArrayList
import java.util.Arrays
import java.util.Collections
import java.util.Comparator

fun sort(list: MutableList<String>, comparator: (String, String) -> Int) {
    Collections.sort(list, object : Comparator<String> {
        override fun compare(p0: String, p1: String) = comparator(p0, p1)
    })
}

fun compare(s1: String, s2: String) = s1 compareTo s2

fun box(): String {
    val l = ArrayList(Arrays.asList("d", "b", "c", "e", "a"))
    sort(l, ::compare)
    if (l != Arrays.asList("a", "b", "c", "d", "e")) return "Fail: $l"
    return "OK"
}
