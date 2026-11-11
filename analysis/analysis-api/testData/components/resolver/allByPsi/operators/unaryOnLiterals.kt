fun simpleValid() {
    +1
    -2
    +3L
    -4L
}

fun invlid() {
    +"5"
    -"6"
}

fun withExpectedType() {
    val b: Byte = -1
    val s: Short = -1
    val l: Long = -1
}

fun minValues() {
    -2147483648
    val b: Byte = -128
}

// IGNORE_STABILITY: candidates
