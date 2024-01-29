// LANGUAGE: +ExpectedTypeGuidedResolution

class Duration(val milliseconds: Int) {
    companion object {
        val Int.seconds: Duration get() = Duration(this)
    }
}

data class Inv<T>(val value: T)

fun <T> foo(output: Inv<T>): Int {
    val v = output.value
    return when {
        v is Duration && v == 1.<!UNRESOLVED_REFERENCE!>seconds<!> -> 0
        else -> 1
    }
}

fun bar(v: Any): Int {
    return when {
        v is Duration && v == 1.seconds -> 0
        else -> 1
    }
}