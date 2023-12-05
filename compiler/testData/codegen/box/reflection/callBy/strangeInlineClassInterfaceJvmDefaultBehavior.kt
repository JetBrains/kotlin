// TARGET_BACKEND: JVM
// WITH_REFLECT
// !JVM_DEFAULT_MODE: all


import kotlin.test.assertEquals
import kotlin.reflect.full.instanceParameter


interface IIC {
    fun f(i1: Int = 1): Int

}

fun box(): String {
    val methods = IIC::class.java.methods.toList()
    require(methods.size == 2) { methods.joinToString("\n") }
    
    return "OK"
}
