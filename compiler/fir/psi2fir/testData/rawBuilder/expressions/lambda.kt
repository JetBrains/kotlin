data class Tuple(val x: Int, val y: Int)

inline fun use(f: (Tuple) -> Int) = f(Tuple(1, 2))

fun foo(): Int {
    val l1 = { t: Tuple ->
        val x = t.x
        val y = t.y
        x + y
    }
    use { (x, y) -> x + y }

    return use {
        if (it.x == 0) return@foo 0
        return@use it.y
    }
}

fun bar(): Int {
    return use lambda@{
        if (it.x == 0) return@bar 0
        return@lambda it.y
    }
}

fun test(list: List<Int>) {
    val map = mutableMapOf<Int, String>()
    list.forEach { map.getOrPut(it, { mutableListOf() }) += "" }
}