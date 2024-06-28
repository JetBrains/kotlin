// FIR_DIFFERENCE
// K1 performs a check for legacy JS BE; however, this is not relevant for IR BE because in the generated JS code,
// there is no name clash between the constructor with JsName and the other top-level declarations with the same JsName.
// Keep K1 as it is, but for K2, implement a more relevant check without the clash.
package foo

class A(val x: String) {
    @JsName("aa") constructor(x: Int) : this("int $x")
}

fun aa() {}
