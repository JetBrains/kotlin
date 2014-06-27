class A(
        var x : Int,
        var y : A? = null) {
}

fun check(a : A?) : Int {
    return (a?.y?.x ?: a?.x) ?: 3
}

fun box() : String {
    val a1 = A(2, A(1))
    val a2 = A(2, null)
    val a3 = null

    val result1 = check(a1)
    val result2 = check(a2)
    val result3 = check(a3)

    if (result1 != 1) return "fail 1: ${result1}"
    if (result2 != 2) return "fail 2: ${result2}"
    if (result3 != 3) return "fail 3: ${result3}"

    return "OK"
}
