fun bar() : Boolean = true

fun foobar(x: Boolean, y: String, z: String) = x.toString() + y + z

inline fun foo() = "-"

fun box() {
    val result = foobar(if (1 == 1) true else bar(), foo(), "OK")
}

// 1 ISTORE
// 3 ILOAD
// 2 ASTORE
// 7 ALOAD
// 3 MAXLOCALS = 3
// 0 InlineMarker
