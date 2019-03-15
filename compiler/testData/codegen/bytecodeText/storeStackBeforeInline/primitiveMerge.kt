// IGNORE_BACKEND: JVM_IR
inline fun <T> runAfterLoop(fn: () -> T): T {
    for (i in 1..2);
    return fn()
}

fun bar() : Boolean = true

fun foobar(x: Boolean, y: String, z: String) = x.toString() + y + z

inline fun foo() = runAfterLoop { "-" }

fun test() {
    val result = foobar(if (1 == 1) true else bar(), foo(), "OK")
}

// fake inline variables occupy 7 ISTOREs.
// 14 ISTORE
// 8 ILOAD
// 2 ASTORE
// 7 ALOAD
// 1 MAXLOCALS = 3
// 1 MAXLOCALS = 4
// 0 InlineMarker
