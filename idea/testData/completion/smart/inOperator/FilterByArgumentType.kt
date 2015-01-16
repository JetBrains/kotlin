trait X {
    fun contains(s: String): Boolean
}

trait Y {
    fun contains(i: Int): Boolean
}

trait Z {
    fun contains(o: Any): Boolean
}

fun foo(s: String, x: X, y: Y, z: Z) {
    if (s in <caret>)
}

// EXIST: x
// ABSENT: y
// EXIST: z
