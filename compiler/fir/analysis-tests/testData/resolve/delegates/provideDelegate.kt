import kotlin.reflect.KProperty

class Delegate<T>(var value: T) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): T = value

    operator fun setValue(thisRef: Any?, property: KProperty<*>, newValue: T) {
        value = newValue
    }
}

class DelegateProvider<T>(val value: T) {
    operator fun provideDelegate(thisRef: Any?, property: KProperty<*>): Delegate<T> = Delegate(value)
}

fun <T> delegate(value: T): DelegateProvider<T> = DelegateProvider(value)

class A {
    val x by delegate(1)
}