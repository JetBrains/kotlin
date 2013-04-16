import java.util.*

fun box(): String {
    val list = ArrayList(Arrays.asList(3, 2, 4, 8, 1, 5))
    val expected = ArrayList(Arrays.asList(8, 5, 4, 3, 2, 1))
    JavaClass.sortIntList(list, { a, b -> b - a })
    return if (list == expected) "OK" else list.toString()
}