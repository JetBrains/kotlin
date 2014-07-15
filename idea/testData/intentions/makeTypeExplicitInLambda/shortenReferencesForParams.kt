fun main() {
    val randomFunction: (x: kotlin.support.AbstractIterator<Int>, y: kotlin.String) -> kotlin.String = {(<caret>x, str) -> str}
}

// WITH_RUNTIME
