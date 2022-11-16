fun compute(sealedClass: SC1): String = when (sealedClass) {
    is SC1.C1 -> "OK"
    SC1.O1 -> "OK"
    is SC1.C2 -> "OK"
    SC1.O2 -> "OK"
}

fun compute(sealedClass: SC2): String = when (sealedClass) {
    is SC2.C1 -> "OK"
    SC2.O1 -> "OK"
    is SC2.C2 -> "OK"
    SC2.O2 -> "OK"
}

fun compute(sealedInterface: SI1): String = when (sealedInterface) {
    is SI1.C1 -> "OK"
    SI1.O1 -> "OK"
    is SI1.I1 -> "OK"
    is SI1.C2 -> "OK"
    SI1.O2 -> "OK"
    is SI1.I2 -> "OK"
}

fun compute(sealedInterface: SI2): String = when (sealedInterface) {
    is SI2.C1 -> "OK"
    SI2.O1 -> "OK"
    is SI2.I1 -> "OK"
    is SI2.C2 -> "OK"
    SI2.O2 -> "OK"
    is SI2.I2 -> "OK"
}

fun compute(sealedInterface: SI3): String = when (sealedInterface) {
    is SI3.C1 -> "OK"
    SI3.O1 -> "OK"
    is SI3.I1 -> "OK"
    is SI3.C2 -> "OK"
    SI3.O2 -> "OK"
    is SI3.I2 -> "OK"
}
