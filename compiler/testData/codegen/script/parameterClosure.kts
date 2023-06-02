// IGNORE_BACKEND_K2: JVM_IR

// param: 10

fun addX(y: Int) = java.lang.Integer.parseInt(args[0]) + y

val rv = addX(3)

// expected: rv: 13
