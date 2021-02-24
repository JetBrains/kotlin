// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY

fun interface GenericToAny<T> {
    fun invoke(x: T): Any
}

fun interface CharToAny {
    fun invoke(x: Char): Any
}

fun interface GenericCharToAny : GenericToAny<Char>, CharToAny

fun withK(fn: GenericCharToAny) = fn.invoke('K').toString()

fun box(): String =
    withK { "O" + it }