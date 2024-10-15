// RUN_PIPELINE_TILL: FRONTEND
// SKIP_TXT
// WITH_STDLIB
import kotlin.experimental.ExperimentalTypeInference

interface MyList<E>
interface MySequence<E>

fun <E> myListOf(e: E): MyList<E> = TODO()

interface C : MyList<String>

fun <E> foo(m: MyList<E>, c: C) {
    if (c === m) {
        val x1: MyList<String> = m.noOverloadResolutionByLambdaReturnType { x ->
            myListOf(x)
        } // ok in K1 and K2

        val x2: MyList<E> = m.noOverloadResolutionByLambdaReturnType { x ->
            myListOf(x)
        } // ok in K2, error in k1

        val y1: MyList<String> = m.limitedFlatMap { x ->
            myListOf(x)
        } // ok in K1 and K2

        val y2: MyList<E> = <!INITIALIZER_TYPE_MISMATCH, TYPE_MISMATCH!>m.limitedFlatMap { x ->
            myListOf(x)
        }<!> // error in K1 and K2
    }
}

fun <T> MyList<T>.noOverloadResolutionByLambdaReturnType(producer: (T) -> MyList<T>): MyList<T> = TODO()

fun <T> MyList<T>.limitedFlatMap(producer: (T) -> MyList<T>): MyList<T> = TODO()

@OptIn(ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName("limitedFlatMapSeq")
fun <T> MyList<T>.limitedFlatMap(producer: (T) -> MySequence<T>): MyList<T> = TODO()
