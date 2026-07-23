// LANGUAGE: +ContextParameters +CallableReferencesToContextual
// TARGET_BACKEND: JVM_IR
// WITH_REFLECT
// ISSUE: KT-86452

import kotlin.reflect.KFunction

fun box(): String {
    context(s: String)
    fun local(suffix: String) = s + suffix

    return context("O") {
        val ref: (String) -> String = ::local
        if (ref("K") != "OK") return@context "FAIL 1: ${ref("K")}"

        try {
            val parameters = (ref as KFunction<*>).parameters
            "FAIL 2: expected reflection on a local contextual function to fail, got parameters $parameters"
        } catch (e: Throwable) {
            "OK"
        }
    }
}
