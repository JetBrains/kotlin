// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +ExpectedTypeGuidedResolution

package test

open class Base {
    val four: Duration = Duration(4)
}

class Duration(val milliseconds: Int) {
    companion object : Base() {
    }
}

fun foo(duration: Duration): Int = when (duration) {
    four -> 1
    else -> 0
}