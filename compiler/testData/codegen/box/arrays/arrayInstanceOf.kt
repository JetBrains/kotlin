// KT-58549

//test [], get and iterator calls
fun test(createIntNotLong: Boolean): String {
    val a = if (createIntNotLong) IntArray(5) else LongArray(5)
    if (a is IntArray) {
        val x = a.iterator()
        var i = 0
        while (x.hasNext()) {
            if (a[i] != x.next()) return "Fail $i"
            i++
        }
        return "O"
    } else if (a is LongArray) {
        val x = a.iterator()
        var i = 0
        while (x.hasNext()) {
            if (a.get(i) != x.next()) return "Fail $i"
            i++
        }
        return "K"
    }
    return "fail"
}

fun box(): String {
    // Only run this test if primitive array `is` checks work (KT-17137)
    if ((intArrayOf() as Any) is Array<*>) return "OK"

    return test(true) + test(false)
}