// WITH_RUNTIME

fun box() : String {
    val a = arrayOfNulls<String>(3)
    a[0] = "a"
    a[1] = "b"
    a[2] = "c"

    var result = 0
    for(i in a.indices) {
      result += i
    }
    if (result != 3) return "FAIL"
    return "OK"
}
