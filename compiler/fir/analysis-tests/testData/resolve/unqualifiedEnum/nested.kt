// LANGUAGE: +ExpectedTypeGuidedResolution

enum class Outer {
    FIRST, SECOND;
}

enum class Inner {
    SECOND, THIRD;
}

fun foo(o: Outer, i: Inner): Int {
    return when (o) {
        _.FIRST -> 1 ;
        _.SECOND -> when (i) {
            _.SECOND -> 2 ;
            _.THIRD -> 3 ;
        }
    }
}

fun bar(o: Outer, i: Inner): Int {
    return when (o) {
        _.FIRST -> 1 ;
        _.SECOND -> {
            fun baz(): Int {
                return when (i) {
                    _.SECOND -> 2 ;
                    _.THIRD -> 3 ;
                }
            }
            baz()
        }
    }
}
