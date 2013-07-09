import java.util.*

fun box(): String {
    val list = ArrayList(Arrays.asList(3, 2, 4, 8, 1, 5))
    val expected = ArrayList(Arrays.asList(8, 5, 4, 3, 2, 1))
    val comparatorFun: (Int, Int) -> Int = { a, b -> b - a }
    Collections.sort(list, Comparator(comparatorFun))
    return if (list == expected) "OK" else list.toString()
}
