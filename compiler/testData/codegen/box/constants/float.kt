fun box(): String {
    if (1F != 1.toFloat()) return "fail 1"
    if (1.0F != 1.0.toFloat()) return "fail 2"
    if (1e1F != 1e1.toFloat()) return "fail 3"
    if (1.0e1F != 1.0e1.toFloat()) return "fail 4"
    if (1e-1F != 1e-1.toFloat()) return "fail 5"
    if (1.0e-1F != 1.0e-1.toFloat()) return "fail 6"

    if (1f != 1.toFloat()) return "fail 7"
    if (1.0f != 1.0.toFloat()) return "fail 8"
    if (1e1f != 1e1.toFloat()) return "fail 9"
    if (1.0e1f != 1.0e1.toFloat()) return "fail 10"
    if (1e-1f != 1e-1.toFloat()) return "fail 11"
    if (1.0e-1f != 1.0e-1.toFloat()) return "fail 12"

    return "OK"
}

