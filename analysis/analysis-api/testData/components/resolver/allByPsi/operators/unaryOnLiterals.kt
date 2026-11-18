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

fun nestedSigns() {
    - -1
    +-1
    val l: Long = - -1
}

fun withParentheses() {
    -(+(-1))
    -(+2147483648)
    val s: Short = -(32768)
    val b: Byte = -(l@ (@Anno 1))
    val b2: Byte = -(+128)
    +(-2147483648)
}

@Target(AnnotationTarget.EXPRESSION)
@Retention(AnnotationRetention.SOURCE)
annotation class Anno
