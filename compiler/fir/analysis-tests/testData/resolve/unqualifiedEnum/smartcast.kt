// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ExpectedTypeGuidedResolution

class Duration(val milliseconds: Int) {
    companion object {
        val four: Duration get() = Duration(4)
    }
}

data class Inv<T>(val value: T)

fun <T> foo(output: Inv<T>): Int {
    val v = output.value
    return when {
        v is Duration && v == <!UNRESOLVED_REFERENCE!>four<!> -> 0
        else -> 1
    }
}

fun bar(v: Any): Int {
    return when {
        v is Duration && v == four -> 0
        else -> 1
    }
}