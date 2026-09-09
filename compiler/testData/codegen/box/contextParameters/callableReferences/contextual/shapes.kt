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
        val mBound: () -> String = Cls()::member
        if (mBound() != "ctx-member") return "FAIL mBound: ${mBound()}"

        val mUnbound: (Cls) -> String = Cls::member
        if (mUnbound(Cls()) != "ctx-member") return "FAIL mUnbound: ${mUnbound(Cls())}"

        val eBound: () -> String = "R"::ext
        if (eBound() != "ctx-ext-R") return "FAIL eBound: ${eBound()}"

        val eUnbound: (String) -> String = String::ext
        if (eUnbound("R") != "ctx-ext-R") return "FAIL eUnbound: ${eUnbound("R")}"

        val s: () -> String = ::single
        if (s() != "ctx-single") return "FAIL single: ${s()}"

        val st: () -> String = Obj::stat
        if (st() != "ctx-stat") return "FAIL stat: ${st()}"

        val tp: () -> String = ::topProp
        if (tp() != "ctx-top") return "FAIL topProp: ${tp()}"

        val mp: () -> String = PropCls()::memberProp
        if (mp() != "ctx-memberProp") return "FAIL memberProp: ${mp()}"

        val ep: () -> String = 42::extProp
        if (ep() != "ctx-extProp-42") return "FAIL extProp: ${ep()}"

        val tcp = ::twoCtxProp
        tcp.set("V")
        if (tcp.get() != "ctx-true-V") return "FAIL twoCtxProp: ${tcp.get()}"
    }
    return "OK"
}
