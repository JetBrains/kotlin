interface X {
    operator fun contains(s: String): Boolean
}

interface Y {
    operator fun contains(i: Int): Boolean
}

interface Z {
    operator fun contains(o: Any): Boolean
}

fun foo(s: String, x: X, y: Y, z: Z) {
    if (s in <caret>)
}

// EXIST: x
// ABSENT: y
// EXIST: z
