// TARGET_BACKEND: JVM
// WITH_REFLECT
// !JVM_DEFAULT_MODE: all


import kotlin.test.assertEquals
import kotlin.reflect.full.instanceParameter


interface IIC {
    fun f(i1: Int = 1): Int

}

inline class IC(val x: Int) : IIC {
    override fun f(i1: Int) = x + i1
}

fun box(): String {
    val methods = IC::class.java.interfaces.first().methods.toList()
    require(methods.size == 2) { methods.joinToString("\n") }
    
    return "OK"
}
