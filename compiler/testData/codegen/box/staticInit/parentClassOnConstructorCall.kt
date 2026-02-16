// IGNORE_BACKEND: JS_IR JS_IR_ES6 WASM_JS WASM_WASI

// FILE: IO.kt
var initOrder = "IO;"

// FILE: F1.kt

private val topLevel = run {
    initOrder += "F1;"
}

open class A(unused: Unit) {
    init { initOrder += "AI;" }
    companion object {
        init {
            initOrder += "A;"
        }
    }
}

open class B(unused: Unit) : A(run { initOrder += "BA;"}) {
    init { initOrder += "BI;" }
    companion object {
        init {
            initOrder += "B;"
        }
    }
}

class C : B(run { initOrder += "CA;"}) {
    init { initOrder += "CI;" }
    companion object {
        init {
            initOrder += "C;"
        }
    }
    fun test() = initOrder;
}

// FILE: F2.kt

private val topLevel = run {
    initOrder += "F2;"
}

fun box() : String {
    val result = C().test()
    if (result != "IO;F2;A;B;C;CA;BA;AI;BI;CI;") return "FAIL: $result"
    return "OK"
}