// WITH_RUNTIME
fun test(): Int = bar { <caret>foo()

fun foo() = 42

fun bar(f: () -> Int) = f()
//-----
// WITH_RUNTIME
fun test(): Int = bar { 
    <caret>foo()
}

fun foo() = 42

fun bar(f: () -> Int) = f()