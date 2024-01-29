// LANGUAGE: +ExpectedTypeGuidedResolution

sealed interface Either {
    class Left: Either
    class Right: Either
}

fun test(x: Either) {
    when {
        x is Left -> 1
        x is Right -> 2
    }
}