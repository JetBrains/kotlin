// MODULE: base
// FILE: base.kt

open class X {
    open val bar: String = "base class"
}

open class Y: X() {
    override val bar: String = "child class"
}

// MODULE: lib(base)
// FILE: A.kt
// VERSION: 1

class Z : X() 

// FILE: B.kt
// VERSION: 2

class Z : Y() 

// MODULE: mainLib(lib)
// FILE: mainLib.kt

fun lib(): String {
    return when {
        Z().bar != "child class" -> "fail 1"
        else -> "OK"
    }
}

// MODULE: main(mainLib)
// FILE: main.kt
fun box(): String = lib()

