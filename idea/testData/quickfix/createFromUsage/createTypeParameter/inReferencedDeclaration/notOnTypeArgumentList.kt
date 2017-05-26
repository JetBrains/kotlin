// "Create type parameter in class 'X'" "false"
// ERROR: No type arguments expected for class X defined in root package in file notOnTypeArgumentList.kt

class X

fun foo(x: <caret>X<String>) {}