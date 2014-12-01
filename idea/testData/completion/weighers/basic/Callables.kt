fun aaGlobalFun(){}
val aaGlobalProp = 1

open class Base {
    fun aaBaseFun(){}
    val aaBaseProp = 1
}

class Derived : Base() {
    fun aaDerivedFun(){}
    val aaDerivedProp = 1

    fun foo() {
        val aaLocalVal = 1
        fun aaLocalFun(){}

        aa<caret>
    }
}

fun Any.aAnyExtensionFun(){}
fun Derived.aaExtensionFun(){}

val Any.aAnyExtensionProp: Int get() = 1
val Derived.aaExtensionProp: Int get() = 1

// ORDER: aaLocalVal
// ORDER: aaLocalFun
// ORDER: aaDerivedProp
// ORDER: aaDerivedFun
// ORDER: aaBaseProp
// ORDER: aaBaseFun
// ORDER: aaExtensionProp
// ORDER: aaExtensionFun
// ORDER: aAnyExtensionProp
// ORDER: aAnyExtensionFun
// ORDER: aaGlobalProp
// ORDER: aaGlobalFun
