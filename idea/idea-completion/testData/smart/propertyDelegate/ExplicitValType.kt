import kotlin.reflect.KProperty

class X1 {
    operator fun getValue(thisRef: C, property: KProperty<*>): CharSequence = ""
}
class X2 {
    operator fun getValue(thisRef: C, property: KProperty<*>): String = ""
}
class X3 {
    operator fun getValue(thisRef: C, property: KProperty<*>): Any = ""
}

class Y1
class Y2

operator fun Y1.getValue(thisRef: C, property: KProperty<*>): String = ""
operator fun Y2.getValue(thisRef: C, property: KProperty<*>): Int = 1

fun createX1() = X1()
fun createX2() = X2()
fun createX3() = X3()
fun createY1() = Y1()
fun createY2() = Y2()

class C {
    val property: CharSequence by <caret>
}

// EXIST: lazy
// EXIST: { itemText: "Delegates.notNull", tailText:"() (kotlin.properties)", typeText: "ReadWriteProperty<Any?, CharSequence>", attributes:"" }
// EXIST: { itemText: "Delegates.observable", tailText:"(initialValue: CharSequence, crossinline onChange: (KProperty<*>, CharSequence, CharSequence) -> Unit) (kotlin.properties)", typeText: "ReadWriteProperty<Any?, CharSequence>", attributes:"" }
// EXIST: { itemText: "Delegates.vetoable", tailText:"(initialValue: CharSequence, crossinline onChange: (KProperty<*>, CharSequence, CharSequence) -> Boolean) (kotlin.properties)", typeText: "ReadWriteProperty<Any?, CharSequence>", attributes:"" }

// EXIST: createX1
// EXIST: createX2
// ABSENT: createX3
// EXIST: createY1
// ABSENT: createY2

// EXIST: X1
// EXIST: X2
// ABSENT: X3
// EXIST: Y1
// ABSENT: Y2
