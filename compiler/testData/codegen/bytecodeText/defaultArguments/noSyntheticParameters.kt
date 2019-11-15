// FILE: Foo.kt

fun foo(param: String = "123") {}


//TODO: align backends

// JVM_TEMPLATES
// 1 LOCALVARIABLE param
// 1 LOCALVARIABLE

// JVM_IR_TEMPLATES
// 2 LOCALVARIABLE param
// 2 LOCALVARIABLE

