// WITH_RUNTIME
// KT-41771

@JvmInline
value class Polynomial<T : Any>(val coefficients: List<T>) {
    constructor(vararg coefficients: T) : this(coefficients.toList())
}
fun box(): String {
    val p = Polynomial("FAIL1", "OK", "FAIL2")
    return p.coefficients[1]
}