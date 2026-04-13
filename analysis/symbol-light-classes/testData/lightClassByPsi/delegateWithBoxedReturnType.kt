interface BaseInt<T> {
    fun int(): T
}

class DelegatingInt(delegate: BaseInt<Int>) : BaseInt<Int> by delegate

interface BaseBoolean<T> {
    fun boolean(): T
}

class DelegatingBoolean(delegate: BaseBoolean<Boolean>) : BaseBoolean<Boolean> by delegate

interface BaseIntProperty<T> {
    val intProperty: T
}

class DelegatingIntProperty(delegate: BaseIntProperty<Int>) : BaseIntProperty<Int> by delegate

interface BaseBooleanProperty<T> {
    val booleanProperty: T
}

class DelegatingBooleanProperty(delegate: BaseBooleanProperty<Boolean>) : BaseBooleanProperty<Boolean> by delegate
