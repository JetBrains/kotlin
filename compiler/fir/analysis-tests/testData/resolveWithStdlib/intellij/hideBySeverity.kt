import kotlin.reflect.KProperty
import kotlin.properties.ReadWriteProperty

// type must be exposed otherwise `provideDelegate` doesn't work
abstract class StoredPropertyBase<T> : ReadWriteProperty<BaseState, T>

abstract class StoredPropertyDerived<T> : StoredPropertyBase<T>() {
    operator fun provideDelegate(thisRef: Any, property: KProperty<*>): StoredPropertyBase<T> {
        return this
    }
}


abstract class BaseState {
    protected abstract fun <PROPERTY_TYPE> propertyDerived(initialValue: PROPERTY_TYPE): StoredPropertyDerived<PROPERTY_TYPE>

    protected abstract fun <PROPERTY_TYPE> propertyBase(initialValue: PROPERTY_TYPE): StoredPropertyBase<PROPERTY_TYPE>
}

abstract class Some : BaseState() {
    val hideBySeverityDerived: MutableSet<Int> by <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>propertyDerived<!>(mutableSetOf())

    val hideBySeverityBase: MutableSet<Int> by propertyBase(mutableSetOf())
}
