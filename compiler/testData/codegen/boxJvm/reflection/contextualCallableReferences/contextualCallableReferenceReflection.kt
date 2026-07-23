// LANGUAGE: +ContextParameters +CallableReferencesToContextual
// TARGET_BACKEND: JVM_IR
// WITH_REFLECT
// ISSUE: KT-86452

import kotlin.reflect.KFunction
import kotlin.reflect.KMutableProperty0

object O {
    context(s: String, c: Char)
    fun foo() = s + c
}

var _x = ""

context(s: String)
var O.bar: String
    get() = _x
    set(v) {
        _x = s + v
    }

fun box(): String {
    context("O", 'K') {
        val fnRef: () -> String = O::foo
        if ((fnRef as KFunction<*>).call() != "OK") return "FAIL 1: ${(fnRef as KFunction<*>).call()}"

        val propRef: KMutableProperty0<String> = O::bar
        if (propRef.getter.call() != "") return "FAIL 2: ${propRef.getter.call()}"
        propRef.setter.call("K")
        if (propRef.getter.call() != "OK") return "FAIL 3: ${propRef.getter.call()}"
    }

    return "OK"
}
