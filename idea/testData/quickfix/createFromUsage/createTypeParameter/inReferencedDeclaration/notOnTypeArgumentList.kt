// "Create type parameter in class 'X'" "false"
// ACTION: Introduce import alias
// ERROR: No type arguments expected for class X

class X

fun foo(x: <caret>X<String>) {}