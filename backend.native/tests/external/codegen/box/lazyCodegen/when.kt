class A (val p: String) {

    val _kind: String = when {
        p == "test" -> "OK"
        else -> "fail"
    }

}

fun box(): String {

    if (A("test")._kind != "OK") return "fail"

    return "OK"
}