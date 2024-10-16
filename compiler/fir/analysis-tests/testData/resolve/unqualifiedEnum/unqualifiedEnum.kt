// LANGUAGE: +ExpectedTypeGuidedResolution

package test

enum class Sample {
    FIRST, SECOND, THIRD;
}

fun trivial(s: Sample): Int {
    return when (s) {
        _.FIRST -> 1
        _.SECOND -> 2
        _.THIRD -> 3
    }
}

fun equalities(s: Sample): Int {
    return when {
        s == _.FIRST -> 1
        s == _.SECOND -> 2
        s == _.THIRD -> 3
        else -> 0
    }
}

class Container {
    val SECOND = test.Sample.SECOND

    fun priority(s: Sample): Int {
        val FIRST = test.Sample.THIRD
        return when (s) {
            FIRST -> 3
            SECOND -> 2
            test.Sample.FIRST -> 1
            else -> 0
        }
    }
}

