// FIX: Change type to mutable
// WITH_RUNTIME
fun test() {
    var list = foo()
    list += 2<caret>
}

fun foo() = listOf(1)