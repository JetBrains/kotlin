// FIR_IDENTICAL

// IGNORE_BACKEND_K2: JS_IR
// IGNORE_BACKEND_K2: JS_IR_ES6

val test1 = arrayOf<String>()
val test2 = arrayOf("1", "2", "3")
val test3 = arrayOf("0", *test2, *test1, "4")
