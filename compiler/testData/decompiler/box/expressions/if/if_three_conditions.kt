fun box(): String {
    val x = 42
    val y = 777
    val z = 3
    if ((x == 42 || y == 777) && z == 3) {
        return "OK"
    } else {
        return "FAIL"
    }
}
