fun f() : Int {
    return 42
}

fun box(): String {
    if (f() == null) {
        return "fail 1"
    }

    if (f() != null) {
        return "OK"
    }

    return "fail 2"
}

// 0 IF
// 0 ACONST_NULL
