public inline fun <T, R> with(receiver: T, block: T.() -> R): R = receiver.block()

fun aaGlobalFun(){}
val aaGlobalProp = 1

open class Base {
    fun aaBaseFun(){}
    val aaBaseProp = 1
}

class Derived : Base(), Common {
    fun aaDerivedFun(){}
    val aaDerivedProp = 1

    fun foo(y: Y) {
        val aaLocalVal = 1
        fun aaLocalFun(){}

        with (y) {
            if (this is Z1 && this is Z2) {
                aa<caret>
            }
        }
    }
}

interface X {
    fun aaX()
}

interface Y : X {
    fun aaY()
}

interface Z1 : Common {
    fun aaaZ1()
    fun aabZ1()
}

interface Z2 {
    fun aaaZ2()
    fun aabZ2()
}

interface Common {
    fun aazCommon()
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
// ORDER: aaaZ1
// ORDER: aaaZ2
// ORDER: aabZ1
// ORDER: aabZ2
// ORDER: aaX
// ORDER: aaYExt
// ORDER: aaXExt
// ORDER: aaDerivedProp
// ORDER: aaDerivedFun
// ORDER: aaBaseProp
// ORDER: aaBaseFun
// ORDER: aaExtensionProp
// ORDER: aaExtensionFun
// ORDER: aazCommon
// ORDER: aaAnyExtensionProp
// ORDER: aaAnyExtensionFun
// ORDER: aaGlobalProp
// ORDER: aaGlobalFun
// ORDER: aaTypeParamExt
