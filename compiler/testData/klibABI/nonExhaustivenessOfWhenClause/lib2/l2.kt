fun computeSC1(sealedClass: SC1): String = when (sealedClass) {
    is SC1.A -> "A"
    is SC1.B -> "B"
}

fun computeSI1(sealedInterface: SI1): String = when (sealedInterface) {
    is SI1.A -> "A"
    is SI1.B -> "B"
}

fun computeE1(enum: E1): String = when (enum) {
    E1.A -> "A"
    E1.B -> "B"
}

fun computeE2(enum: E2): String = when (enum) {
    E2.A -> "A"
    E2.B -> "B"
}

fun computeSC2(sealedClass: SC2): String = when (sealedClass) {
    is SC2.ClassToObject -> "ClassToObject"
    SC2.ObjectToClass -> "ObjectToClass"
}

fun computeSI2(sealedInterface: SI2): String = when (sealedInterface) {
    is SI2.ClassToObject -> "ClassToObject"
    SI2.ObjectToClass -> "ObjectToClass"
}
