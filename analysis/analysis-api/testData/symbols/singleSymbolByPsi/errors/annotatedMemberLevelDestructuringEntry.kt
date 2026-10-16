// KT-74654
// KT-90199
// LOOK_UP_FOR_ELEMENT_OF_TYPE: KtDestructuringDeclarationEntry
annotation class Ann(val s: String)

const val x = "str"

data class X(val a: Int, val b: Int)

class B {
    val (@Ann(x) a, <caret>b) = X(1, 2)
}
