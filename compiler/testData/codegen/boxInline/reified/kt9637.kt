// IGNORE_BACKEND: JVM_IR
// TARGET_BACKEND: JVM
// FILE: 1.kt
// WITH_REFLECT
package test

import java.util.*
import kotlin.reflect.KClass

val valuesInjectFnc = HashMap<KClass<out Any>, Any>()

inline fun <reified T : Any> injectFnc(): Lazy<Function0<T>> = lazy(LazyThreadSafetyMode.NONE) {
    (valuesInjectFnc[T::class] ?: throw Exception("no inject ${T::class.simpleName}")) as Function0<T>
}

inline fun <reified T : Any> registerFnc(noinline value: Function0<T>) {
    valuesInjectFnc[T::class] = value
}

public class Box

// FILE: 2.kt

//NO_CHECK_LAMBDA_INLINING
import test.*

class Boxer {
    val box: () -> Box by injectFnc()
}

fun box(): String {
    val box = Box()
    registerFnc { box }
    val prop = Boxer().box
    if (prop() != box) return "fail 1"

    return "OK"
}
