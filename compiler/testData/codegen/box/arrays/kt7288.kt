// KT-58549

fun test(b: Boolean): String {
    val a = if (b) IntArray(5) else LongArray(5)
    if (a is IntArray) {
        val x = a.iterator()
        var i = 0
        while (x.hasNext()) {
            if (a[i] != x.next()) return "Fail $i"
            i++
        }
        return "OK"
    } else if (a is LongArray) {
        val x = a.iterator()
        var i = 0
        while (x.hasNext()) {
            if (a[i] != x.next()) return "Fail $i"
            i++
        }
        return "OK"
    }
    return "fail"
}

fun box(): String {
    // Only run this test if primitive array `is` checks work (KT-17137)
    if ((intArrayOf() as Any) is Array<*>) return "OK"

    if (test(true) != "OK") return "fail 1: ${test(true)}"

    if (test(false) != "OK") return "fail 1: ${test(false)}"

    return "OK"
}