// FIR_IDENTICAL
// WITH_STDLIB
// WITH_RUNTIME

interface A1<X1>
interface A2<X2>
interface A3<X2>
interface Res1<Y>
interface Res2<Z>

class Controller<T> {
    fun yield(t: T) {}
}

fun <S> generate(g: suspend Controller<S>.() -> Unit): S = TODO()

fun main(src: A1<String>, res1: Res1<Int>) {
    generate {
        foo(src) { }
            // At this stage we've got E type variable not fixed
            // And two `bar` overloads one of which we should choose depending on the return type
            // But we can't _simply_ start lambda analysis with ConstraintSystemCompletionMode.UNTIL_FIRST_LAMBDA
            // because currently it only allows lambda analysis for completely ready
            .bar { y ->
                res1
            }

        yield("")
    }.length
}

fun <E> foo(x: A1<E>, block: () -> Unit): A2<A3<E>> = TODO()

fun <V1> A2<V1>.bar(transform: (A2<V1>) -> Res1<Int>) {}

@OptIn(kotlin.experimental.ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName("bar1")
fun <V2> A2<V2>.bar(transform: (A2<V2>) -> Res2<Int>) {}