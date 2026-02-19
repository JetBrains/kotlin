// WITH_REFLECT

// Simplified original example from KT-58825

import kotlin.reflect.*

interface DynamicShapeRegister<T> {
    fun register(vararg items: KProperty<*>) {}
    fun register(vararg items: KCallable<*>) {}
}

class C : DynamicShapeRegister<Int>
val p: Int = 0

// Additional tests with nested arrays

interface ArrayDimensions {
    fun getD(x: Array<Int>): String = "1D"
    fun getD(x: Array<Array<Int>>): String = "2D"
    fun getD(x: Array<Array<Array<Int>>>): String = "3D"
    fun getD(x: Array<Array<Array<Array<Int>>>>): String = "4D"
}

fun box(): String {
    C().register(::p, ::p)
    C().register(::box, ::box, ::C)

    val ad: ArrayDimensions = object : ArrayDimensions {}
    check("1D" == ad.getD(arrayOf(1)))
    check("2D" == ad.getD(arrayOf(arrayOf(1))))
    check("3D" == ad.getD(arrayOf(arrayOf(arrayOf(1)))))
    check("4D" == ad.getD(arrayOf(arrayOf(arrayOf(arrayOf(1))))))

    return "OK"
}