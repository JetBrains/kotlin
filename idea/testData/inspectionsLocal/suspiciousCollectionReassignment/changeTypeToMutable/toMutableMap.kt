// FIX: Change type to mutable
// WITH_RUNTIME
fun toMutableMap() {
    var map = foo()
    map += 3 to 4<caret>
}

fun foo() = mapOf(1 to 2)