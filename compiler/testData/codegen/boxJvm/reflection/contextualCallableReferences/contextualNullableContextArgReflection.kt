// LANGUAGE: +ContextParameters +CallableReferencesToContextual
// TARGET_BACKEND: JVM_IR
// WITH_REFLECT
// ISSUE: KT-86452

import kotlin.reflect.KFunction
import kotlin.reflect.KProperty0

context(c: String?)
fun orDefault(): String = c ?: "default"

context(c: String?)
val display: String
    get() = c ?: "none"

fun box(): String {
    val f = context<String?, () -> String>(null) { ::orDefault }
    val r1 = (f as KFunction<*>).call()
    if (r1 != "default") return "FAIL 1: $r1"

    val g = context<String?, () -> String>("x") { ::orDefault }
    val r2 = (g as KFunction<*>).call()
    if (r2 != "x") return "FAIL 2: $r2"

    val p = context<String?, KProperty0<String>>(null) { ::display }
    val r3 = p.getter.call()
    if (r3 != "none") return "FAIL 3: $r3"

    return "OK"
}
