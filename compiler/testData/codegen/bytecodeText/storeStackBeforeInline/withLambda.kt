inline fun <T> runAfterLoop(fn: () -> T): T {
    for (i in 1..2);
    return fn()
}

inline fun bar(x: String, block: (String) -> String) = runAfterLoop { "def" + block(x) }

fun foobar(x: String, y: String, z: String) = x + y + z

fun foo() : String {
    return foobar("abc", bar("ghi") { x -> x + "jkl" }, "mno")
}

// JVM_TEMPLATES
// 6 ASTORE
// 18 ALOAD
// 1 MAXLOCALS = 7
// 0 InlineMarker

// JVM_IR_TEMPLATES
// 4 ASTORE
// 16 ALOAD
// 1 MAXLOCALS = 6
// 0 InlineMarker
