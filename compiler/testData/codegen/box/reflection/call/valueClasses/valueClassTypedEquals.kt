// TARGET_BACKEND: JVM_IR
// WITH_REFLECT
// LANGUAGE: +ValueClasses, +CustomEqualsInValueClasses

import kotlin.reflect.KClass
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.memberFunctions

@JvmInline
value class Vc(val x: Int, val y: Int) {
    @TypedEquals
    fun equals(other: Vc): Boolean {
        return (x - other.x) % 2 == 0 && (y - other.y) % 2 == 0
    }
}

fun box(): String {
    val typedEq = Vc::class.memberFunctions.single { it.hasAnnotation<TypedEquals>() }
    if (typedEq.call(Vc(0, 0), Vc(1, 0)) == true) return "Fail 1"
    if (typedEq.call(Vc(0, 0), Vc(2, 2)) == false) return "Fail 2"
    return "OK"
}