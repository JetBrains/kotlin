import kotlin.reflect.KProperty

class X<T>(t: T) {
    operator fun getValue(thisRef: C<T>, property: KProperty<*>): T = throw Exception()
    operator fun setValue(thisRef: C<T>, property: KProperty<*>, t: T) {}
}

class C<T> {
    var property by <caret>
}

// EXIST: { itemText: "X", tailText: "(t: T) (<root>)" }
