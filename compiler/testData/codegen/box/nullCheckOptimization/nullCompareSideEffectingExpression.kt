var result = "fail1"

fun f() : Int {
    result = "OK"
    return 42
}

fun box(): String {
    if (f() == null) {
        return "fail2"
    }

    if (result != "OK") {
        return "fail3"
    }

    result = "fail4"
    if (f() != null) {
        return result
    }

    return result
}

// 1 IF
// 0 IFNULL
// 0 IFNONNULL
// 0 ACONST_NULL
