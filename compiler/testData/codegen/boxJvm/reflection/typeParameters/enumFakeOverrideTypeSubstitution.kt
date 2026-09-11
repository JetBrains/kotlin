// TARGET_BACKEND: JVM
// WITH_REFLECT
// Tests that fake override methods inherited from Enum<E> have their type parameter E
// correctly substituted with the concrete enum type in javaType.

import kotlin.reflect.full.*
import kotlin.reflect.jvm.javaType
import kotlin.test.*

enum class Season { SPRING, SUMMER, AUTUMN, WINTER }
enum class Weekday(val abbreviation: String) {
    MON("Mon"), TUE("Tue"), WED("Wed"), THU("Thu"), FRI("Fri")
}

fun checkEnumFakeOverrides(enumClass: kotlin.reflect.KClass<out Enum<*>>, simpleName: String) {
    val fns = enumClass.memberFunctions.associateBy { it.name }

    // getDeclaringClass() is inherited from Enum<E>; return type should be Class<EnumType>
    val getDeclaringClass = fns["getDeclaringClass"]
    if (getDeclaringClass != null) {
        val jt = getDeclaringClass.returnType.javaType.typeName
        assertTrue(
            jt.contains(simpleName) || jt == "java.lang.Class<${enumClass.java.name}>",
            "$simpleName.getDeclaringClass() return javaType should reference $simpleName, got: $jt"
        )
        assertFalse(jt == "java.lang.Class<E>" || jt.endsWith("<E>"),
            "$simpleName.getDeclaringClass() must not use raw type variable E, got: $jt")
    }

    // compareTo(E other) — parameter should be the concrete enum type, not E
    val compareTo = fns["compareTo"]
    if (compareTo != null) {
        val paramType = compareTo.valueParameters.firstOrNull()?.type?.javaType?.typeName
        if (paramType != null) {
            assertFalse(paramType == "E",
                "$simpleName.compareTo() parameter must not be raw 'E', got: $paramType")
        }
    }
}

fun box(): String {
    checkEnumFakeOverrides(Season::class,  "Season")
    checkEnumFakeOverrides(Weekday::class, "Weekday")
    return "OK"
}
