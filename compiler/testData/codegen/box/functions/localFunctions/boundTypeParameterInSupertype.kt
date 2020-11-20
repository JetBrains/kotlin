interface I<TT> {
    fun id(x: TT) = x
}

fun <T> forClass(x: T): T {
    fun g(z: T): T {
        class C : I<T>

        return C().id(z)
    }

    return g(x)
}

fun <T> forTypeParameter(x: T): T {
    fun <S : T> h(z: S) = z

    return h(x)
}

fun box() = forClass("O") + forTypeParameter("K")
