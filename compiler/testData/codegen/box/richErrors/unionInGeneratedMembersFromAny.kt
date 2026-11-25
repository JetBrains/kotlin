// LANGUAGE: +RichErrors +FullValueClasses
error class E(val x: String | RichError)
value class V(val x: String | E)
data class D(val x: String | E)

fun box(): String {
    return V("O").x as String + (D(E("K")).x as E).x as String
}
