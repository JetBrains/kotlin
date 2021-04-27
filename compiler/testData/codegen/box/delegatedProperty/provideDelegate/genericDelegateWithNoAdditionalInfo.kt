// IGNORE_BACKEND_FIR: JVM_IR
// WITH_RUNTIME

import kotlin.reflect.KProperty

interface DelegateProvider<out T> {
    operator fun provideDelegate(receiver: Any?, prop: kotlin.reflect.KProperty<*>): Lazy<T>
}
fun <Value : Any> delegate(): DelegateProvider<Value> = object : DelegateProvider<Value> {
    override fun provideDelegate(receiver: Any?, prop: KProperty<*>): Lazy<Value> {
        return lazy { "OK" } as Lazy<Value>
    }
}

fun box(): String {
    val value: String by delegate()
    return value
}
