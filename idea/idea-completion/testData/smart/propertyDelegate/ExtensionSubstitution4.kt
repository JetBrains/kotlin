import kotlin.reflect.KProperty

class Property<TOwner1, TValue1>(owner: TOwner1, value: TValue1)

operator fun <TValue2, TOwner2> Property<TOwner2, TValue2>.getValue(thisRef: TOwner2, property: KProperty<*>): TValue2 {
    throw Exception()
}

operator fun <TValue3, TOwner3> Property<TOwner3, TValue3>.setValue(thisRef: TOwner3, property: KProperty<*>, value: TValue3) {
    throw Exception()
}

fun<TOwner4, TValue4> createProperty(owner: TOwner4, value: TValue4): Property<TOwner4, TValue4> = Property(owner, value)

class C {
    var v: Int by <caret>
}

// EXIST: { itemText: "createProperty", typeText: "Property<C, Int>" }
// EXIST: { itemText: "Property", tailText: "(owner: C, value: Int) (<root>)" }
