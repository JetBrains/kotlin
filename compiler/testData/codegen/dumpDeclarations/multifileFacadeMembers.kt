// WITH_STDLIB
// FILE: Part1.kt
@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("MultifileFacade")
// properties

public val publicVal: Int = 1
public var publicVar: Int = 1
internal val internalVal: Long = 1
internal var internalVar: Long = 1
private val privateVal: Any? = 1
private var privateVar: Any? = 1

//// fields
//@JvmField public val publicValField: Int = 1
//@JvmField public var publicVarField: Int = 1
//@JvmField internal val internalValField: Long = 1
//@JvmField internal var internalVarField: Long = 1


// FILE: Part2.kt
@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("MultifileFacade")

// constants

public const val publicConst: Int = 2
internal const val internalConst: Int = 3
private const val privateConst: Int = 4

// fun

public fun publicFun() {}
internal fun internalFun(param1: Int) {}
private fun privateFun(x: Any) {}


private class PrivateClass {
    internal fun accessUsage() {
        privateFun(privateConst)
    }

}
