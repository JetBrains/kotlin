public inline fun <T, R> with(receiver: T, block: T.() -> R): R = receiver.block()

fun aaGlobalFun(){}
val aaGlobalProp = 1

open class Base {
    fun aaBaseFun(){}
    val aaBaseProp = 1
}

class Derived : Base() {
    fun aaDerivedFun(){}
    val aaDerivedProp = 1

    fun foo(y: Y) {
        val aaLocalVal = 1
        fun aaLocalFun(){}

        with (y) {
            aa<caret>
        }
    }
}

interface X {
    fun aaX()
}
interface Y : X {
    fun aaY()
}

fun Any.aaAnyExtensionFun(){}
fun Derived.aaExtensionFun(){}

val Any.aaAnyExtensionProp: Int get() = 1
val Derived.aaExtensionProp: Int get() = 1

fun <T> T.aaTypeParamExt(){}

fun X.aaXExt(){}
fun Y.aaYExt(){}

// ORDER: aaLocalVal
// ORDER: aaLocalFun
// ORDER: aaY
// ORDER: aaX
// ORDER: aaYExt
// ORDER: aaXExt
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
