// LANGUAGE: +ContextSensitiveEnumResolutionInWhen
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

fun shouldNotWork(s: Sample): Int {
    return when {
        s == <!UNRESOLVED_REFERENCE!>FIRST<!> -> 1
        s == <!UNRESOLVED_REFERENCE!>SECOND<!> -> 2
        s == <!UNRESOLVED_REFERENCE!>THIRD<!> -> 3
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

