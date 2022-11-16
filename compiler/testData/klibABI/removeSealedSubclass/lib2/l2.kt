fun compute(sealedClass: SC1): String = when (sealedClass) {
    is SC1.C1 -> "OK"
    SC1.O1 -> "OK"
    is SC1.Removed -> "FAIL1"
    is SC1.C2 -> "FAIL2"
    SC1.O2 -> "FAIL3"
}

fun compute(sealedClass: SC2): String = when (sealedClass) {
    is SC2.C1 -> "OK"
    SC2.O1 -> "OK"
    SC2.Removed -> "FAIL4"
    is SC2.C2 -> "FAIL5"
    SC2.O2 -> "FAIL6"
}

fun compute(sealedInterface: SI1): String = when (sealedInterface) {
    is SI1.C1 -> "OK"
    SI1.O1 -> "OK"
    is SI1.I1 -> "OK"
    is SI1.Removed -> "FAIL7"
    is SI1.C2 -> "FAIL8"
    SI1.O2 -> "FAIL9"
    is SI1.I2 -> "FAIL10"
}

fun compute(sealedInterface: SI2): String = when (sealedInterface) {
    is SI2.C1 -> "OK"
    SI2.O1 -> "OK"
    is SI2.I1 -> "OK"
    SI2.Removed -> "FAIL11"
    is SI2.C2 -> "FAIL12"
    SI2.O2 -> "FAIL13"
    is SI2.I2 -> "FAIL14"
}

fun compute(sealedInterface: SI3): String = when (sealedInterface) {
    is SI3.C1 -> "OK"
    SI3.O1 -> "OK"
    is SI3.I1 -> "OK"
    is SI3.Removed -> "FAIL15"
    is SI3.C2 -> "FAIL16"
    SI3.O2 -> "FAIL17"
    is SI3.I2 -> "FAIL18"
}
