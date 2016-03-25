import kotlin.reflect.KProperty

class Property<TOwner, TValue>

operator fun <TValue, TOwner> Property<TOwner, TValue>.getValue(thisRef: TOwner, property: KProperty<*>): TValue {
    throw Exception()
}

fun<TOwner, TValue> createProperty(): Property<TOwner, TValue> = Property()

class C {
    val v: Int by <caret>
}

// EXIST: { itemText: "createProperty", typeText: "Property<C, Int>" }
// EXIST: Property
