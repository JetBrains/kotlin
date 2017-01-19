// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

fun box(): String {
    val a = LongArray(5)
    val x = a.iterator()
    var i = 0
    while (x.hasNext()) {
        if (a[i] != x.nextLong()) return "Fail $i"
        i++
    }
    return "OK"
}
