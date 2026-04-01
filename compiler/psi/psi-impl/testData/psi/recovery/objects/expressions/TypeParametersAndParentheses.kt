// COMPILATION_ERRORS

val foo = object<T, R>(x: Int) {}

val foo = object<T, R>() : Bar {

}

val foo = object<T, R>()
