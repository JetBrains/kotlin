import kotlin.reflect.KProperty

class X {
    operator fun getValue(thisRef: C, property: KProperty<*>): String = ""
}

class C {
    val property by <caret>
}

// WITH_ORDER
// EXIST: { itemText: "Delegates.notNull", tailText:"() (kotlin.properties)", typeText: "ReadWriteProperty<Any?, T>", attributes:"" }
// EXIST: { itemText: "Delegates.observable", tailText:"(initialValue: T, crossinline onChange: (KProperty<*>, T, T) -> Unit) (kotlin.properties)", typeText: "ReadWriteProperty<Any?, T>", attributes:"" }
// EXIST: { itemText: "Delegates.vetoable", tailText:"(initialValue: T, crossinline onChange: (KProperty<*>, T, T) -> Boolean) (kotlin.properties)", typeText: "ReadWriteProperty<Any?, T>", attributes:"" }
// EXIST: lazy
// EXIST: X
