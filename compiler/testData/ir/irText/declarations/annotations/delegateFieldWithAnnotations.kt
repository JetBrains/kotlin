// FIR_IDENTICAL
// WITH_RUNTIME

annotation class Ann

@delegate:Ann
val test1 by lazy { 42 }