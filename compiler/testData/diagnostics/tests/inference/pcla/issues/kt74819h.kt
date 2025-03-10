// FIR_IDENTICAL
// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-74819
// WITH_STDLIB

import kotlin.experimental.ExperimentalTypeInference

fun <T, R, T1> Iterable<T>.otherFlatMap(transform: (T) -> Iterable<R>, x: List<T1>): List<R> = TODO()

@JvmName("foo")
@OptIn(ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
fun <T, R, T1> Iterable<T>.otherFlatMap(transform: (T) -> Sequence<R>, x: List<T1>): List<R> = TODO()

fun <X> myListOf(): List<X> = TODO()

fun main() {
    buildList {
        add("")
        otherFlatMap({ listOf("") }, this)
    }
}
