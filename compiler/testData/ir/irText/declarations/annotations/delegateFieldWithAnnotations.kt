// FIR_IDENTICAL
// WITH_STDLIB

annotation class Ann

@delegate:Ann
val test1 by lazy { 42 }
