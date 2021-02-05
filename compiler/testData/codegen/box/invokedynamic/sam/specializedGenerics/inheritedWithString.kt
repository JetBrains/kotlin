// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY

fun interface GenericToAny<T> {
    fun invoke(x: T): Any
}

fun interface GenericStringToAny : GenericToAny<String>

fun withK(fn: GenericStringToAny) = fn.invoke("K").toString()

fun box(): String =
    withK { "O" + it }