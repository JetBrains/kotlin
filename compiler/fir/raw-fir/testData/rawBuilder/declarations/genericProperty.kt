fun <T> genericFoo(): T = TODO()

val <T> T.generic: T get() = genericFoo()