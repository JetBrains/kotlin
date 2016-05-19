interface X
interface Y : X
interface Z

operator fun X.contains(s: String): Boolean = true
fun Z.contains(s: String): Boolean = true

fun foo(s: String, x: X, y: Y, z: Z) {
    if (s in <caret>)
}

// EXIST: x
// EXIST: y
// ABSENT: z
