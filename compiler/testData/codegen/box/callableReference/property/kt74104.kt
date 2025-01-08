// WITH_STDLIB
// FILE: CO.kt
import kotlin.reflect.*

class ConstOk(val x: Any?) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>) : String {
        if (x == null) return "fail: x is null"
        if (x !is KProperty0<*>) return "fail: x is not a KProperty0"
        return "OK"
    }
}

// FILE: A.kt

object A {
    val x: String by ConstOk(A::x)
}

// FILE: B.kt

object B {
    val x: String by ConstOk(this::x)
}

// FILE: C.kt

class C {
    val x: String by ConstOk(this::x)
}

// FILE: D.kt

val x: String by ConstOk(::x)


// FILE: main.kt

fun box(): String {
    val res = "${A.x};${B.x};${C().x};$x"
    if (res != "OK;OK;OK;OK") return res
    return "OK"
}