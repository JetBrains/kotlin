// FIX: Change type to mutable
// WITH_RUNTIME
fun test() {
    var set = foo()
    set += 1<caret>
}

fun foo() = setOf(1)