// WITH_RUNTIME
fun test(): Int = bar { f<caret>oo()

fun foo() = 42

fun bar(f: () -> Int) = f()
//-----
// WITH_RUNTIME
fun test(): Int = bar { f
    <caret>oo()

fun foo() = 42

fun bar(f: () -> Int) = f()