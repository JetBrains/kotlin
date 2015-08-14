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

fun Any.aaAnyExtensionFun(){}
fun Derived.aaExtensionFun(){}

val Any.aaAnyExtensionProp: Int get() = 1
val Derived.aaExtensionProp: Int get() = 1

fun <T> T.aaTypeParamExt(){}

// ORDER: aaLocalVal
// ORDER: aaLocalFun
// ORDER: aaDerivedProp
// ORDER: aaDerivedFun
// ORDER: aaBaseProp
// ORDER: aaBaseFun
// ORDER: aaExtensionProp
// ORDER: aaExtensionFun
// ORDER: aaAnyExtensionProp
// ORDER: aaAnyExtensionFun
// ORDER: aaGlobalProp
// ORDER: aaGlobalFun
// ORDER: aaTypeParamExt
