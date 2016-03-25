import kotlin.reflect.KProperty

class X1 {
    operator fun getValue(thisRef: C, property: KProperty<*>): String = ""
    operator fun setValue(thisRef: C, property: KProperty<*>, value: String) {}
}

class X2 {
    operator fun getValue(thisRef: C, property: KProperty<*>): String = ""
}

class X3 {
    operator fun getValue(thisRef: C, property: KProperty<*>): String = ""
    operator fun setValue(thisRef: String, property: KProperty<*>, value: String) {}
}

class X4 {
    operator fun getValue(thisRef: C, property: KProperty<*>): String = ""
    operator fun setValue(thisRef: C, property: KProperty<*>, value: CharSequence) {}
}

class X5 {
    operator fun getValue(thisRef: C, property: KProperty<*>): CharSequence = ""
    operator fun setValue(thisRef: C, property: KProperty<*>, value: String) {}
}

class Y1
class Y2

operator fun Y1.getValue(thisRef: C, property: KProperty<*>): String = ""
operator fun Y1.setValue(thisRef: C, property: KProperty<*>, value: String) {}
operator fun Y2.getValue(thisRef: C, property: KProperty<*>): String = ""

fun createX1() = X1()
fun createX2() = X2()
fun createX3() = X3()
fun createX4() = X4()
fun createX5() = X5()

class C {
    var property by <caret>
}

// ABSENT: lazy
// EXIST: { itemText: "Delegates.notNull", tailText:"() (kotlin.properties)", typeText: "ReadWriteProperty<Any?, T>", attributes:"" }
// EXIST: { itemText: "Delegates.observable", tailText:"(initialValue: T, crossinline onChange: (KProperty<*>, T, T) -> Unit) (kotlin.properties)", typeText: "ReadWriteProperty<Any?, T>", attributes:"" }
// EXIST: { itemText: "Delegates.vetoable", tailText:"(initialValue: T, crossinline onChange: (KProperty<*>, T, T) -> Boolean) (kotlin.properties)", typeText: "ReadWriteProperty<Any?, T>", attributes:"" }

// EXIST: createX1
// ABSENT: createX2
// ABSENT: createX3
// EXIST: createX4
// ABSENT: createX5

// EXIST: X1
// ABSENT: X2
// ABSENT: X3
// EXIST: X4
// ABSENT: X5
// EXIST: Y1
// ABSENT: Y2
