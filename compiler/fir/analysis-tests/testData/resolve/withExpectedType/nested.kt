// LANGUAGE: +ExpectedTypeGuidedResolution

sealed interface Boo {
    data class Baa(val x: String): Boo
}

// 'when' conditions
fun boo(b: Boo): String = when (b) {
    is Baa -> "hello"
}

// value initialization
val x: Boo = Baa("a")