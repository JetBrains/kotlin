// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// ISSUE: KT-72437
// WITH_STDLIB

external interface I01 {
    fun some(x: Int = definedExternally, y: Int)
}

external open class C01 {
    open fun some(x: Int = definedExternally, y: Int = definedExternally)
}

external class C02: C01, I01

fun main(){
    C02().some()
}
