package test

class ExtValInClass<P> {
    val Int.asas: P?
        get() = throw Exception()
}
