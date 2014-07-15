fun box(): String {
    val d = 42.0
    val c = 'C'

    open class Local(val l: Long) {
        fun foo(): Boolean = d == 42.0 && c == 'C' && l == 239L
    }

    if (object : Local(239L) {
        fun bar(): Boolean = foo()
    }.bar()) return "OK"

    return "Fail"
}
