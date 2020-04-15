// IGNORE_BACKEND: JS_IR
// TODO: investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// WITH_REFLECT

import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.*

class C {
    var prop = 42
}

val C_propReflect = C::class.memberProperties.find { it.name == "prop" } as? KMutableProperty1 ?: throw AssertionError()
val C_prop = C::prop
val cProp = C()::prop

fun box() =
        when {
            C_prop.getter != C_prop.getter -> "C_prop.getter != C_prop.getter"
            C_propReflect.getter != C_propReflect.getter -> "C_propReflect.getter != C_propReflect.getter"
            cProp.getter != cProp.getter -> "cProp.getter != cProp.getter"

            cProp.getter == C_prop.getter -> "cProp.getter == C_prop.getter"
            C_prop.getter == cProp.getter -> "C_prop.getter == cProp.getter"
            cProp.getter == C_propReflect.getter -> "cProp.getter == C_propReflect.getter"
            C_propReflect.getter == cProp.getter -> "C_propReflect.getter == cProp.getter"

            // TODO https://youtrack.jetbrains.com/issue/KT-13490
            // cProp.getter != C()::prop.getter -> "cProp.getter != C()::prop.getter"
            // cProp.setter != C()::prop.setter -> "cProp.setter != C()::prop.setter"

            else -> "OK"
        }
