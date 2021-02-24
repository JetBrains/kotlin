class A<S> {
    fun x(a: kotlin.Int, b: kotlin.String): Unit {}

    fun <T> x(a: kotlin.Int, b: java.lang.String): kotlin.Int { TODO() }

    fun <T, Q> x(a: kotlin.collections.Map<T, kotlin.collections.Map<in S, out Q>>, b: kotlin.collections.List<kotlin.Byte>): kotlin.Int { TODO() }

    fun <T, Q> kotlin.collections.List<kotlin.collections.Map<T, Q>>.x(f: MyType<S>): kotlin.Int { TODO() }
}

class MyType<in F>