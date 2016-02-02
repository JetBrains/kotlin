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
// EXIST: createX1
// EXIST: createX2
// ABSENT: createX3
// EXIST: createY1
// ABSENT: createY2
/*TODO: add constructors*/
