// TARGET_BACKEND: JVM_IR
// LANGUAGE: +NewShapeForFirstLastFunctionsInKotlinList

class A<E> : AbstractList<E>() {
    override fun get(index: Int): E {
        TODO("Not yet implemented")
    }

    override val size: Int
        get() = TODO("Not yet implemented")

    override fun first(): E {
        return "OK" as E
    }
}

fun stringList(): List<String> = A()

fun box(): String {
    return stringList().first()
}
