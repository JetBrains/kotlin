// SHOULD_FAIL_WITH: Property <b><code>genericValT</code></b> references type parameters of the containing class
class Test6<T>(val t: T) {
    val <caret>genericValT: T get() = t
}