fun putNumberCompareAsUnit() {
    if (1 == 1) {
    }
    else if (1 == 1) {
    }
}

fun putNumberCompareAsVoid() {
    if (1 == 1) {
        1 == 1
    } else {
    }
}

fun putInvertAsUnit(b: Boolean) {
    if (1 == 1) {
    } else if (!b) {
    }
}

fun box(): String {
    putNumberCompareAsUnit()
    putNumberCompareAsVoid()
    putInvertAsUnit(true)
    return "OK"
}
