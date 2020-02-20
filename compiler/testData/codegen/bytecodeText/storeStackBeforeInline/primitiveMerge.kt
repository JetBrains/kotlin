inline fun <T> runAfterLoop(fn: () -> T): T {
    for (i in 1..2);
    return fn()
}

fun bar() : Boolean = true

fun foobar(x: Boolean, y: String, z: String) {}

inline fun foo() = runAfterLoop { "-" }

fun test() {
    val result = foobar(if (1 == 1) true else bar(), foo(), "OK")
}

// 2 ASTORE
// 5 ALOAD
// 1 MAXLOCALS = 3
// 1 MAXLOCALS = 4
// 0 InlineMarker

// JVM_TEMPLATES
// fake inline variables occupy 7 ISTOREs.
// 14 ISTORE
// 7 ILOAD

// JVM_IR_TEMPLATES
// JVM_IR generates an extra induction variable in 'for (i in 1..2)' (see KT-36837)
// 17 ISTORE
// 10 ILOAD
