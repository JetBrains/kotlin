fun foo(a: (Int) -> Int): Int = a(1)
val x = foo { it + foo { <caret>it } }