// LANGUAGE: +ContextParameters +CallableReferencesToContextual
// TARGET_BACKEND: JVM
// WITH_REFLECT
// ISSUE: KT-86452

import kotlin.reflect.KFunction
import kotlin.reflect.KParameter

context(c: String)
fun topFun(): String = "$c-topFun"

context(c: String)
fun withDefault(x: Int = 7): String = "$c-$x"

context(c: String)
fun twoArgs(first: Int, second: String): String = "$c-$first-$second"

class Cls {
    context(c: String)
    fun member(): String = "$c-member"
}

context(c: String)
val topProp: String get() = "$c-topProp"

var storage: String = ""

context(c: String, b: Boolean)
var twoCtxProp: String
    get() = storage
    set(value) { storage = "$c-$b-$value" }

context(c: String)
fun String.extFun(x: Int): String = "$c-$this-$x"

context(c: String)
val String.extProp: String get() = "$c-$this-extProp"

context(c: String, b: Boolean)
var String.twoCtxExtProp: String
    get() = storage + this
    set(value) { storage = "$c-$b-$this-$value" }

fun box(): String {
    context("ctx") {
        val tf = ::topFun
        if (tf.call() != "ctx-topFun") return "FAIL 1: ${tf.call()}"
        if (tf.parameters.isNotEmpty()) return "FAIL 2: ${tf.parameters.map { it.kind }}"

        val tp = ::topProp
        if (tp.getter.call() != "ctx-topProp") return "FAIL 3: ${tp.getter.call()}"
        if (tp.getter.parameters.isNotEmpty()) return "FAIL 4: ${tp.getter.parameters.map { it.kind }}"

        val wd = ::withDefault
        if (wd.parameters.size != 1 || wd.parameters[0].kind != KParameter.Kind.VALUE) return "FAIL 5: ${wd.parameters.map { it.kind }}"
        if (wd.parameters[0].name != "x") return "FAIL 6: ${wd.parameters[0].name}"
        if (wd.callBy(emptyMap()) != "ctx-7") return "FAIL 7: ${wd.callBy(emptyMap())}"
        if (wd.callBy(mapOf(wd.parameters[0] to 5)) != "ctx-5") return "FAIL 8: ${wd.callBy(mapOf(wd.parameters[0] to 5))}"

        val ta = ::twoArgs
        if (ta.parameters.map { it.kind } != listOf(KParameter.Kind.VALUE, KParameter.Kind.VALUE)) return "FAIL 9: ${ta.parameters.map { it.kind }}"
        if (ta.parameters.map { it.name } != listOf("first", "second")) return "FAIL 10: ${ta.parameters.map { it.name }}"
        if (ta.call(3, "Z") != "ctx-3-Z") return "FAIL 11: ${ta.call(3, "Z")}"

        val m = Cls::member
        if (m.parameters.size != 1) return "FAIL 12: ${m.parameters.map { it.kind }}"
        if (m.parameters[0].kind != KParameter.Kind.INSTANCE) return "FAIL 13: ${m.parameters[0].kind}"
        if (m.parameters[0].name != null) return "FAIL 14: ${m.parameters[0].name}"
        if (m.call(Cls()) != "ctx-member") return "FAIL 15: ${m.call(Cls())}"

        val ef = String::extFun
        if (ef.parameters.map { it.kind } != listOf(KParameter.Kind.EXTENSION_RECEIVER, KParameter.Kind.VALUE)) return "FAIL 16: ${ef.parameters.map { it.kind }}"
        if (ef.call("R", 1) != "ctx-R-1") return "FAIL 17: ${ef.call("R", 1)}"

        val bef = "R"::extFun
        if (bef.parameters.map { it.kind } != listOf(KParameter.Kind.VALUE)) return "FAIL 18: ${bef.parameters.map { it.kind }}"
        if (bef.parameters[0].name != "x") return "FAIL 19: ${bef.parameters[0].name}"
        if (bef.call(1) != "ctx-R-1") return "FAIL 20: ${bef.call(1)}"

        val ep = String::extProp
        if (ep.getter.parameters.map { it.kind } != listOf(KParameter.Kind.EXTENSION_RECEIVER)) return "FAIL 21: ${ep.getter.parameters.map { it.kind }}"
        if (ep.getter.call("R") != "ctx-R-extProp") return "FAIL 22: ${ep.getter.call("R")}"

        val bep = "R"::extProp
        if (bep.getter.parameters.isNotEmpty()) return "FAIL 23: ${bep.getter.parameters.map { it.kind }}"
        if (bep.getter.call() != "ctx-R-extProp") return "FAIL 24: ${bep.getter.call()}"
    }

    context("ctx", true) {
        val p2 = ::twoCtxProp
        if (p2.getter.parameters.isNotEmpty()) return "FAIL 25: ${p2.getter.parameters.map { it.kind }}"
        p2.setter.call("V")
        if (p2.getter.call() != "ctx-true-V") return "FAIL 26: ${p2.getter.call()}"

        val ep2 = String::twoCtxExtProp
        if (ep2.setter.parameters.map { it.kind } != listOf(KParameter.Kind.EXTENSION_RECEIVER, KParameter.Kind.VALUE)) return "FAIL 27: ${ep2.setter.parameters.map { it.kind }}"
        ep2.setter.call("R", "V")
        if (ep2.getter.call("!") != "ctx-true-R-V!") return "FAIL 28: ${ep2.getter.call("!")}"

        val bep2 = "S"::twoCtxExtProp
        if (bep2.setter.parameters.map { it.kind } != listOf(KParameter.Kind.VALUE)) return "FAIL 29: ${bep2.setter.parameters.map { it.kind }}"
        bep2.setter.call("W")
        if (bep2.getter.call() != "ctx-true-S-WS") return "FAIL 30: ${bep2.getter.call()}"
    }

    return "OK"
}
