// !CHECK_TYPE

fun <T> bar(f: () -> T) : T = f()

fun test(map: MutableMap<Int, Int>) {
    val r = bar {
        map[1] = 2
    }
    r checkType { _<Unit>() }
}