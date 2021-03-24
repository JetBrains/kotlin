// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: IGNORED_IN_JS
// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6
// TODO: investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// WITH_REFLECT

import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KMutableProperty2
import kotlin.reflect.full.*

class C {
    var prop = 42
    var String.prop: Int
        get() = 42
        set(value) {}
}

val cProp = C()::prop
val C_prop = C::prop
val C_propReflect = C::class.memberProperties.find { it.name == "prop" } as? KMutableProperty1 ?: throw AssertionError()
val C_extPropReflect = C::class.memberExtensionProperties.find { it.name == "prop" } as? KMutableProperty2 ?: throw AssertionError()

fun box() =
    when {
        C_prop.getter != C_propReflect.getter -> "C_prop.getter != C_propReflect.getter"
        C_prop.setter != C_propReflect.setter -> "C_prop.setter != C_propReflect.setter"
        C_propReflect.getter != C_prop.getter -> "C_propReflect.getter != C_prop.getter"
        C_propReflect.setter != C_prop.setter -> "C_propReflect.setter != C_prop.setter"

        // reflexive test of acessors of KProperty0, KProperty1, KProperty2
        cProp.getter != cProp.getter -> "cProp.getter != cProp.getter"
        cProp.setter != cProp.setter -> "cProp.setter != cProp.setter"
        C_prop.getter != C_prop.getter -> "C_prop.getter != C_prop.getter"
        C_prop.setter != C_prop.setter -> "C_prop.setter != C_prop.setter"
        C_propReflect.getter != C_propReflect.getter -> "C_propReflect.getter != C_propReflect.getter"
        C_propReflect.setter != C_propReflect.setter -> "C_propReflect.setter != C_propReflect.setter"
        C_extPropReflect.getter != C_extPropReflect.getter -> "C_extPropReflect.getter != C_extPropReflect.getter"
        C_extPropReflect.setter != C_extPropReflect.setter -> "C_extPropReflect.setter != C_extPropReflect.setter"

        // acessors of KProperty0, Kproperty1 and Kproperty2 are not equal to each other
        cProp.getter == C_prop.getter -> "cProp.getter == C_prop.getter"
        cProp.setter == C_prop.setter -> "cProp.setter == C_prop.setter"
        C_prop.getter == cProp.getter -> "C_prop.getter == cProp.getter"
        C_prop.setter == cProp.setter -> "C_prop.setter == cProp.setter"
        cProp.getter == C_propReflect.getter -> "cProp.getter == C_propReflect.getter"
        cProp.setter == C_propReflect.setter -> "cProp.setter == C_propReflect.setter"
        C_propReflect.getter == cProp.getter -> "C_propReflect.getter == cProp.getter"
        C_propReflect.setter == cProp.setter -> "C_propReflect.setter == cProp.setter"

        cProp.getter == C_extPropReflect.getter -> "cProp.getter == C_extPropReflect.getter"
        cProp.setter == C_extPropReflect.setter -> "cProp.setter == C_extPropReflect.setter"
        C_extPropReflect.getter == cProp.getter -> "C_extPropReflect.getter == cProp.getter"
        C_extPropReflect.setter == cProp.setter -> "C_extPropReflect.setter == cProp.setter"

        C_prop.getter == C_extPropReflect.getter -> "C_prop.getter == C_extPropReflect.getter"
        C_prop.setter == C_extPropReflect.setter -> "C_prop.setter == C_extPropReflect.setter"
        C_extPropReflect.getter == C_prop.getter -> "C_extPropReflect.getter == C_prop.getter"
        C_extPropReflect.setter == C_prop.setter -> "C_extPropReflect.setter == C_prop.setter"

        else -> "OK"
    }
