import kotlin.reflect.KProperty

class Property<TOwner1, TValue1>

operator fun <TValue2, TOwner2> Property<TOwner2, TValue2>.getValue(thisRef: TOwner2, property: KProperty<*>): TValue2 {
    throw Exception()
}

operator fun <TValue3, TOwner3> Property<TOwner3, TValue3>.setValue(thisRef: TOwner3, property: KProperty<*>, value: TValue3) {
    throw Exception()
}

fun<TOwner4, TValue4> createProperty(): Property<TOwner4, TValue4> = Property()

class C {
    var v by <caret>
}

// EXIST: { itemText: "createProperty", typeText: "Property<C, TValue4>" }
// EXIST: Property
