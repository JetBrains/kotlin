fun forWithIf() {
    for (a in 1..5) {
        if (!(a > 0 && a < 3 || a >= 4 && a < 10)) {
            42
        }
        else {
            43
        }
    }
    44
}

fun nestedIf() {
    for (a in -1..6) {
        if (a < 5) {
            if (a > 0) {
                if (a == 1) {
                    41
                }
                else {
                    if (a == 2) {
                        42
                    }
                }
            }
            43
        }
        else {
            44
        }
        45
    }
}