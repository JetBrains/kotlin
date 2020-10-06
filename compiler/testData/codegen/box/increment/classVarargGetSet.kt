object A {
    var x = 0
    var gets = 0
    var sets = 0

    operator fun get(vararg va: Int): Int {
        for (i in va) {
            gets += i
        }
        return x
    }

    operator fun set(vararg va: Int, value: Int) {
        for (i in va) {
            sets += i
        }
        x = value
    }
}

fun box(): String {
    A.x = 0
    val xx = A[1, 2, 3]++
    if (xx != 0) return "Failed xx: $xx"
    if (A.x != 1) return "Failed A.x: ${A.x}"
    if (A.gets != 6) return "Failed A.gets: ${A.gets}"
    if (A.sets != 6) return "Failed A.sets: ${A.sets}"
    return "OK"
}
