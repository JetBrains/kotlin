// LANGUAGE: +ContextParameters +CallableReferencesToContextual

class Cls {
    context(c: String)
    fun member(): String = "$c-member"
}

context(c: String)
fun String.ext(): String = "$c-ext-$this"

context(c: String)
fun single(): String = "$c-single"

object Obj {
    context(c: String)
    fun stat(): String = "$c-stat"
}

context(c: String)
val topProp: String get() = "$c-top"

class PropCls {
    context(c: String)
    val memberProp: String get() = "$c-memberProp"
}

context(c: String)
val Int.extProp: String get() = "$c-extProp-$this"

var storage: String = ""

context(c: String, b: Boolean)
var twoCtxProp: String
    get() = storage
    set(value) { storage = "$c-$b-$value" }

fun box(): String {
    context("ctx", true) {
        // --- function: regular-class member ---
        val mBound: () -> String = Cls()::member
        if (mBound() != "ctx-member") return "FAIL mBound: ${mBound()}"

        val mUnbound: (Cls) -> String = Cls::member
        if (mUnbound(Cls()) != "ctx-member") return "FAIL mUnbound: ${mUnbound(Cls())}"

        // --- function: extension (single bound context argument; the extension receiver is bound/unbound) ---
        val eBound: () -> String = "R"::ext
        if (eBound() != "ctx-ext-R") return "FAIL eBound: ${eBound()}"

        val eUnbound: (String) -> String = String::ext
        if (eUnbound("R") != "ctx-ext-R") return "FAIL eUnbound: ${eUnbound("R")}"

        // --- function: single bound context argument, no receiver ---
        val s: () -> String = ::single
        if (s() != "ctx-single") return "FAIL single: ${s()}"

        // --- function: object member (the @JvmStatic variant is in jvmStaticObjectContextualFunctionRef.kt) ---
        val st: () -> String = Obj::stat
        if (st() != "ctx-stat") return "FAIL stat: ${st()}"

        // --- property: top-level (get) ---
        val tp: () -> String = ::topProp
        if (tp() != "ctx-top") return "FAIL topProp: ${tp()}"

        // --- property: regular-class member (get) ---
        val mp: () -> String = PropCls()::memberProp
        if (mp() != "ctx-memberProp") return "FAIL memberProp: ${mp()}"

        // --- property: extension (get) ---
        val ep: () -> String = 42::extProp
        if (ep() != "ctx-extProp-42") return "FAIL extProp: ${ep()}"

        // --- property: two context parameters (get + set) ---
        val tcp = ::twoCtxProp
        tcp.set("V")
        if (tcp.get() != "ctx-true-V") return "FAIL twoCtxProp: ${tcp.get()}"
    }
    return "OK"
}
