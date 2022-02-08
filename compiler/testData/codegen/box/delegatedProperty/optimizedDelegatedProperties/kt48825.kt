// IGNORE_BACKEND: JVM
// MODULE: lib
// FILE: lib.kt
import kotlin.reflect.KProperty

open class A {
    inline operator fun Int.getValue(thisRef: Any?, property: KProperty<*>): String =
        property.name
}

// MODULE: main(lib)
// FILE: main.kt
import kotlin.reflect.KProperty

open class B : A() {
    var result = "fail"

    inline operator fun String.getValue(thisRef: Any?, property: KProperty<*>): String =
        property.name

    inline operator fun String.setValue(thisRef: Any?, property: KProperty<*>, value: String) {
        result = this + property.name
    }
}

class C : B() {
    val O by 1
    var K by O
}

fun box() = C().run { K = "..."; result }
