interface X
interface Y : X
interface Z

fun X.contains(s: String): Boolean

fun foo(s: String, x: X, y: Y, z: Z) {
    if (s in <caret>)
}

// EXIST: x
// EXIST: y
// ABSENT: z
