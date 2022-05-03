// WITH_STDLIB

import kotlin.reflect.KProperty

class OptionDescriptor<T>
interface ArgumentValueDelegate<T> {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): T = listOf("OK") as T
}

abstract class CLIEntity<TResult> constructor(val delegate: ArgumentValueDelegate<TResult>) {
    operator fun provideDelegate(thisRef: Any?, prop: KProperty<*>): ArgumentValueDelegate<TResult> = delegate
}

abstract class AbstractSingleOption<T : Any, TResult> constructor(delegate: ArgumentValueDelegate<TResult>) : CLIEntity<TResult>(delegate)
class ArgumentSingleNullableValue<T : Any>(descriptor: OptionDescriptor<T>): ArgumentValueDelegate<T?>
class SingleNullableOption<T : Any> constructor(descriptor: OptionDescriptor<T>) : AbstractSingleOption<T, T?>(ArgumentSingleNullableValue(descriptor))

fun box(): String {
    val x: List<Any>? by SingleNullableOption(OptionDescriptor())
    return x?.get(0).toString()
}
