// TARGET_BACKEND: JVM
// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_K2: ANY
// It was necessary to add TARGET_BACKEND because native part became flacky for some reason
// TODO remove TARGET_BACKEND limitation once the issue is fixed.
// ISSUE: KT-84998
// LANGUAGE: +EagerLambdaAnalysis

fun <T> materialize(): T = "OK" as T

fun bar(x: () -> Comparable<*>): String {
    return x().toString()
}

fun bar(w: Int = 1, x: () -> Comparable<*>): String {
    return TODO()
}

var myFalseValue = false

fun box(): String {
    return bar {
        when {
            myFalseValue -> 1
            else -> materialize()
        }
    }
}
