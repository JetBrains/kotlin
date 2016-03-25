import kotlin.reflect.KProperty

class Property<TOwner, TValue>(owner: TOwner, value: TValue)

operator fun <TValue, TOwner> Property<TOwner, TValue>.getValue(thisRef: TOwner, property: KProperty<*>): TValue {
    throw Exception()
}

fun<TOwner, TValue> createProperty(owner: TOwner, value: TValue): Property<TOwner, TValue> = Property(owner, value)

class C {
    val v: Int by <caret>
}

// EXIST: { itemText: "createProperty", typeText: "Property<C, Int>" }
// EXIST: { itemText: "Property", tailText: "(owner: C, value: Int) (<root>)" }
