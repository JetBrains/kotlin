fun box(): String {

    return when(val x = 2; x) {
        in (1..3) -> "OK"
        else -> "fail"
    }
}
