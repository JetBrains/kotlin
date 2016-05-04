import kotlin.reflect.KProperty
import kotlin.reflect.KProperty1

class DVal<T, R, P: KProperty1<T, R>>(val kmember: P) {
    operator fun getValue(t: T, p: KProperty<*>): R {
        return kmember.get(t)
    }
}

class Value<T>(var text: String? = null)

class Test {
    val <T> Value<T>.additionalText by DVal(Value<T>::text)
}

// field: Test::additionalText$delegate
// jvm signature: LDVal;
// generic signature: null