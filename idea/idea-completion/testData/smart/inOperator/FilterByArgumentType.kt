interface X {
    fun contains(s: String): Boolean
}

interface Y {
    fun contains(i: Int): Boolean
}

interface Z {
    fun contains(o: Any): Boolean
}

fun foo(s: String, x: X, y: Y, z: Z) {
    if (s in <caret>)
}

// EXIST: x
// ABSENT: y
// EXIST: z
