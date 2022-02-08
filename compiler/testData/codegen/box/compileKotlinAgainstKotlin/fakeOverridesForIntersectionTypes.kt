// TARGET_BACKEND: JVM
// MODULE: lib
// FILE: A.kt
package a

interface T {
    var x: String
}

interface A : T {
    override var x: String
}

interface B : T {
    override var x: String
}

class C : A, B {
    override var x: String = ""
}

class D : A, B {
    override var x: String = ""
}

// MODULE: main(lib)
// FILE: B.kt
import a.*

fun foo(condition: Boolean): String {
    val aAndB = if (condition) C() else D()
    aAndB.x = "OK"

    return aAndB.x
}

fun box(): String {
    if (foo(true) != "OK") return "fail 1"
    if (foo(false) != "OK") return "fail 2"

    return "OK"
}
