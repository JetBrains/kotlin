// FILE: Anno.kt
@Target(AnnotationTarget.EXPRESSION)
@Retention(AnnotationRetention.SOURCE)
annotation class Anno

// FILE: check.kt
import kotlin.contracts.*

@OptIn(ExperimentalContracts::class)
fun Boolean.myCheck(x: Boolean): Boolean {
    @Anno
    contract { returns() implies (x) }
    return x
}
