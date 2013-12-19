fun box(): String {
    if (check(1, { it as Int }) == "OK") return "fail 1"
    if (check(1, { it as Byte }) != "OK") return "fail 2"
    if (check(1, { it as Short }) != "OK") return "fail 3"
    if (check(1, { it as Long }) != "OK") return "fail 4"
    if (check(1, { it as Char }) != "OK") return "fail 5"
    if (check(1, { it as Double }) != "OK") return "fail 6"
    if (check(1, { it as Float }) != "OK") return "fail 7"

    if (check(1.0, { it as Int }) != "OK") return "fail 11"
    if (check(1.0, { it as Byte }) != "OK") return "fail 12"
    if (check(1.0, { it as Short }) != "OK") return "fail 13"
    if (check(1.0, { it as Long }) != "OK") return "fail 14"
    if (check(1.0, { it as Char }) != "OK") return "fail 15"
    if (check(1.0, { it as Double }) == "OK") return "fail 16"
    if (check(1.0, { it as Float }) != "OK") return "fail 17"

    if (check(1f, { it as Int }) != "OK") return "fail 21"
    if (check(1f, { it as Byte }) != "OK") return "fail 22"
    if (check(1f, { it as Short }) != "OK") return "fail 23"
    if (check(1f, { it as Long }) != "OK") return "fail 24"
    if (check(1f, { it as Char }) != "OK") return "fail 25"
    if (check(1f, { it as Double }) != "OK") return "fail 26"
    if (check(1f, { it as Float }) == "OK") return "fail 27"

    return "OK"
}

fun check<T>(param: T, f: (T) -> Unit): String {
    try {
        f(param)
    }
    catch (e: ClassCastException) {
        return "OK"
    }
    return "fail"
}

