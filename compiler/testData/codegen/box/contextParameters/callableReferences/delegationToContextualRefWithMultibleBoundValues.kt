// LANGUAGE: +ContextParameters +CallableReferencesToContextual
// WITH_STDLIB

context(c1: Int, c2: String)
val contextualizedProp: String get() = c2

fun box(): String = with("OK") {
    with(1) {
        class B {
            val y by ::contextualizedProp
        }
        B().y
    }
}
