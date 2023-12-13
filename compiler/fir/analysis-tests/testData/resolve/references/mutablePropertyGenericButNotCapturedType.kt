class Generic<T>
class Klass<T> {
    var mutableProperty: Generic<T> = Generic()

    fun testWithinClass() {
        val mutableProperty = Klass<T>::mutableProperty
        mutableProperty.set(this, Generic<T>())
    }
}

fun testConcreteType() {
    val mutableProperty = Klass<Int>::mutableProperty
    mutableProperty.set(Klass<Int>(), Generic<Int>())
}

fun <A> testGenericType() {
    val mutableProperty = Klass<A>::mutableProperty
    mutableProperty.set(Klass<A>(), Generic<A>())
}

fun <S: CharSequence> testGenericTypeWithBounds() {
    val mutableProperty = Klass<S>::mutableProperty
    mutableProperty.set(Klass<S>(), Generic<S>())
}