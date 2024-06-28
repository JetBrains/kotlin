// LANGUAGE: +ContextSensitiveEnumResolutionInWhen
enum class Outer {
    FIRST, SECOND;
}

enum class Inner {
    SECOND, THIRD;
}

fun foo(o: Outer, i: Inner): Int {
    return when (o) {
        FIRST -> 1
        SECOND -> when (i) {
            SECOND -> 2
            THIRD -> 3
        }
    }
}

fun bar(o: Outer, i: Inner): Int {
    return when (o) {
        FIRST -> 1
        SECOND -> {
            fun baz(): Int {
                return when (i) {
                    SECOND -> 2
                    THIRD -> 3
                }
            }
            baz()
        }
    }
}
