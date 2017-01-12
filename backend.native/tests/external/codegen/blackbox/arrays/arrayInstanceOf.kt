// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

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
    return test(true) + test(false)
}