import kotlin.reflect.KProperty

class X1 {
    operator fun getValue(thisRef: C, property: KProperty<*>): String = ""
    operator fun setValue(thisRef: C, property: KProperty<*>, value: String) {}
}

class X2 {
    operator fun getValue(thisRef: C, property: KProperty<*>): String = ""
    operator fun setValue(thisRef: C, property: KProperty<*>, value: CharSequence) {}
}

class X3 {
    operator fun getValue(thisRef: C, property: KProperty<*>): CharSequence = ""
    operator fun setValue(thisRef: C, property: KProperty<*>, value: String) {}
}

fun createX1() = X1()
fun createX2() = X2()
fun createX3() = X3()

class C {
    var property: CharSequence by <caret>
}

// ABSENT: lazy
// ABSENT: createX1
// EXIST: createX2
// ABSENT: createX3
