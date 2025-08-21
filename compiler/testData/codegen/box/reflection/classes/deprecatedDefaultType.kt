// TARGET_BACKEND: JVM
// WITH_REFLECT
// FULL_JDK

package test

import kotlin.reflect.full.defaultType
import kotlin.reflect.jvm.javaType
import java.lang.reflect.ParameterizedType

class C<X, Y, Z : X>

fun box(): String {
    val type = C::class.defaultType
    if (type.classifier != C::class) return "Fail classifier: ${type.classifier}"

    val typeParams = C::class.typeParameters
    val typeArgs = type.arguments.map { it.type!!.classifier }
    if (typeParams != typeArgs) return "Fail args: $typeArgs"

    if (type.toString() != "test.C<X, Y, Z>") return "Fail toString: $type"

    val javaType = type.javaType
    if (javaType !is ParameterizedType ||
        javaType.rawType != C::class.java ||
        javaType.toString() != "test.C<X, Y, Z>"
    ) return "Fail javaType: $javaType (${javaType::class.java})"

    return "OK"
}
