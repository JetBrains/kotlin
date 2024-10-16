// LANGUAGE: +ExpectedTypeGuidedResolution

sealed interface Either {
    class Left: Either
    class Right: Either
}

fun test(x: Either) {
    when {
        x is _.Left -> 1
        x is _.Right -> 2
    }
}