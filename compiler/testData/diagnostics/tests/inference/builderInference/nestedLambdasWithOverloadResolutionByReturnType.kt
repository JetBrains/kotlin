// FIR_IDENTICAL
// WITH_STDLIB

class Controller<T> {
    fun yield(t: T): Boolean = true
}

fun <S> generate(g: suspend Controller<S>.() -> Unit): S = TODO()

fun foo(x1: List<String>, x2: List<String>) {
    generate {
        yield(
            x1.flatMap1 {
                x2.map2 { "" }
            }
        )
    }.length
}

fun <T1> Iterable<T1>.flatMap1(transform: (T1) -> String): String = TODO()

@OptIn(kotlin.experimental.ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName("flatMapSequence")
fun <T2> Iterable<T2>.flatMap1(transform: (T2) -> Int): Int = TODO()

fun <T3, R3> T3.map2(transform: (T3) -> R3): R3 = TODO()