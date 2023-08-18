// FIR_IDENTICAL
// ISSUE: KT-57076
// WITH_STDLIB

interface I01 {
    fun some(x: Int = 1, y: Int)
}

open class C01 {
    open fun some(x: Int = -1, y: Int = 2) {
        println("x = $x y = $y")
    }
}

<!MULTIPLE_DEFAULTS_INHERITED_FROM_SUPERTYPES_WHEN_NO_EXPLICIT_OVERRIDE!>class C02<!>: C01(), I01

fun main(){
    C02().some() // K2: x = -1 y = 2
}
