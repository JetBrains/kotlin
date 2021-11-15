// WITH_STDLIB

inline fun <T> put(
        x: T,
        maxExclusive: Int,
        isEmpty: (Int) -> Boolean,
        equals: (T, T) -> Boolean,
        fetch: (Int) -> T,
        store: (Int, T) -> Unit
): Boolean {
    var i = 0
    do {
        if (isEmpty(i)) {
            store(i, x)
            return true
        }

        val y = fetch(i)
        if (equals(x, y)) {
            return false
        }

        i++
        if (i >= maxExclusive) return false
    } while (true)
}

const val SIZE = 16
val arr = IntArray(SIZE) { -1 }

fun putNonNegInt(x: Int) =
        put(x, SIZE,
            isEmpty = { arr[it] < 0 },
            equals = { x, y -> x == y },
            fetch = { arr[it] },
            store = { i, x -> arr[i] = x }
        )

fun box(): String {
    putNonNegInt(1)
    putNonNegInt(2)
    putNonNegInt(3)

    if (arr[0] != 1) return "Fail, ${arr.toList().toString()}"
    if (arr[1] != 2) return "Fail, ${arr.toList().toString()}"
    if (arr[2] != 3) return "Fail, ${arr.toList().toString()}"

    return "OK"
}