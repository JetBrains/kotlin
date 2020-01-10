// IGNORE_BACKEND_FIR: JVM_IR
// IGNORE_BACKEND: NATIVE
// IGNORE_BACKEND: JS_IR
//For KT-6020

// MODULE: lib
// FILE: lib.kt
import kotlin.reflect.KProperty1
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty

class Value<T>(var value: T = null as T, var text: String? = null)

val <T> Value<T>.additionalText by DVal(Value<T>::text) //works

val <T> Value<T>.additionalValue by DVal(Value<T>::value) //not work

class DVal<T, R, P: KProperty1<T, R>>(val kmember: P) {
    operator fun getValue(t: T, p: KProperty<*>): R {
        return kmember.get(t)
    }
}

// MODULE: main(lib)
// FILE: main.kt
fun box(): String {
    val p = Value("O", "K")
    return p.additionalValue + p.additionalText
}
