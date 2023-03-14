// TARGET_BACKEND: JVM_IR
// WITH_REFLECT
// LANGUAGE: +ValueClasses, +CustomEqualsInValueClasses

import kotlin.reflect.KClass
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.memberFunctions

@JvmInline
value class Ic(val x: Int) {
    @TypedEquals
    fun equals(other: Ic): Boolean {
        return (x - other.x) % 2 == 0
    }
}

fun box(): String {
    val typedEq = Ic::class.memberFunctions.single { it.hasAnnotation<TypedEquals>() }
    if (typedEq.call(Ic(1), Ic(2)) == true) return "Fail 1"
    if (typedEq.call(Ic(1), Ic(3)) == false) return "Fail 2"
    return "OK"
}