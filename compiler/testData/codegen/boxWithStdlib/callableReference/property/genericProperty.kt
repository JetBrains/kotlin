//For KT-6020
import kotlin.reflect.KProperty1
import kotlin.reflect.KMutableProperty1

class Value<T>(var value: T = null as T, var text: String? = null)

val <T> Value<T>.additionalText by DVal(Value<T>::text) //works

val <T> Value<T>.additionalValue by DVal(Value<T>::value) //not work

class DVal<T, R, P: KProperty1<T, R>>(val kmember: P) {
    fun get(t: T, p: PropertyMetadata): R {
        return kmember.get(t)
    }
}

fun box(): String {
    val p = Value("O", "K")
    return p.additionalValue + p.additionalText
}
