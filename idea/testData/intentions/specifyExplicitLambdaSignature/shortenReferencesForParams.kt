fun main() {
    val randomFunction: (x: kotlin.properties.ObservableProperty<Int>, y: kotlin.String) -> kotlin.String = { <caret>x, str -> str}
}

// WITH_RUNTIME
