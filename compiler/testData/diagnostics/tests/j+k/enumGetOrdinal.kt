// DISABLE_JAVA_FACADE
// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// WITH_STDLIB
import kotlin.experimental.ExperimentalTypeInference

fun interface MyRunnable {
    fun run();
}

fun interface MyCallable<V> {
    fun call(): V
}

fun submit(x: MyRunnable) {}
fun <VS> submit(x: MyCallable<VS>) {}

@OptIn(ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
fun submit1(x: () -> Unit) {}
@OptIn(ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
fun submit1(x: () -> String): String = ""


fun main() {
    //submit { "" }

    submit1 { "" }.length
}
