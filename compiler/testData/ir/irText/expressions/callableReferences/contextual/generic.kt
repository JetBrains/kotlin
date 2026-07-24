// LANGUAGE: +ContextParameters +CallableReferencesToContextual
// IGNORE_BACKEND: JVM_IR
// ^KT-86452
import kotlin.reflect.*

context(t: A)
fun <A, B, C, D> B.foo(p: C): D? = null

@Suppress("INCORRECT_TYPE_PARAMETER_OF_PROPERTY")
context(t: A)
var <A, B, C> B.bar: C?
    get() = null
    set(v) {}

fun String.test() {
    val x: Int.(Boolean) -> Long? = Int::foo
    val y: KMutableProperty0<Long?> = 1::bar
    val z: KMutableProperty1<Int, Long?> = Int::bar
}
