// LANGUAGE: +ExpectedTypeGuidedResolution

package test

enum class Sample {
    FIRST, SECOND, THIRD;
}

fun trivial(s: Sample): Int {
    return when (s) {
        FIRST -> 1
        SECOND -> 2
        THIRD -> 3
    }
}

fun equalities(s: Sample): Int {
    return when {
        s == FIRST -> 1
        s == SECOND -> 2
        s == THIRD -> 3
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

