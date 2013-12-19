fun box(): String {
    val a: Long = 2147483647 + 1
    if (a != -2147483648L) return "fail: in this case we should add to ints and than cast the result to long - overflow expected"
    return "OK"
}