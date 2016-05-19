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

fun createX1() = X1()
fun createX2() = X2()
fun createX3() = X3()
fun createX4() = X4()
fun createX5() = X5()

class C

var C.property by <caret>

// ABSENT: lazy

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
