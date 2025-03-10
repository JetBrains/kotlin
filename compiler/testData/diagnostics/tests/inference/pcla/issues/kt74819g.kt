// FIR_IDENTICAL
// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-74819
// WITH_STDLIB
// FULL_JDK
// JVM_TARGET: 1.8

import java.util.*
import kotlin.experimental.ExperimentalTypeInference

fun <T, R> Iterable<T>.optionalFlatMap(transform: (Optional<T>) -> Iterable<R>): List<R> = TODO()

@JvmName("foo")
@OptIn(ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
fun <T, R> Iterable<T>.optionalFlatMap(transform: (Optional<T>) -> Sequence<R>): List<R> = TODO()

fun main() {
    buildList {
        add("")
        optionalFlatMap { listOf("") }
    }
}
