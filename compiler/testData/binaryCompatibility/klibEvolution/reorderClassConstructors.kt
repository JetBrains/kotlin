// MODULE: lib
// FILE: A.kt
// VERSION: 1

open class X(open val y: String) {
    constructor(x: String, y: String): this(x+y)
    constructor(x: String, y: String, z: String): this(x, y+z)
}

// FILE: B.kt
// VERSION: 2

open class X(x: String, _y: String, z: String) {
    constructor(x: String): this("fifth", x, "sixth")
    constructor(y: String, z: String): this(y+z, "tenth", "eleventh")
    val y = _y
}

// MODULE: mainLib(lib)
// FILE: mainLib.kt

open class Y: X("nineth") 
open class Z: X("seventh", "eighth") 
open class W: X("first", "second", "third")

fun lib(): String {
    val y = Y()
    val z = Z()
    val w = W()
    return when {
        y.y != "nineth" -> "fail 1"
        z.y != "tenth" -> "fail 2"
        w.y != "second" -> "fail 3"

        else -> "OK"
    }
}

// MODULE: main(mainLib)
// FILE: main.kt
fun box(): String = lib()

