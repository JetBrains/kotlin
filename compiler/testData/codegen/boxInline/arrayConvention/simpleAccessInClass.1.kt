import test.*


fun box(): String {

    with(A()) {
        val z = 1;

        val p = z[2, 3]
        if (p != 6) return "fail 1: $p"

        z[2, 3] = p
        if (res != 12) return "fail 2: $res"
    }

    return "OK"
}