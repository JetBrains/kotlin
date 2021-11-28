// WITH_STDLIB
// KT-41771

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class Polynomial<T : Any>(val coefficients: List<T>) {
    constructor(vararg coefficients: T) : this(coefficients.toList())
}
fun box(): String {
    val p = Polynomial("FAIL1", "OK", "FAIL2")
    return p.coefficients[1]
}