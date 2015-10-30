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