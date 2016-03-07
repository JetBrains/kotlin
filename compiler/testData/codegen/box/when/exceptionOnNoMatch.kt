fun isZero(x: Int) = when(x) {
    0 -> true
    else -> throw Exception()
}

fun box(): String {
    try {
	isZero(1)
    }
    catch (e: Exception) {
        return "OK"
    }
    return "Fail"
}
