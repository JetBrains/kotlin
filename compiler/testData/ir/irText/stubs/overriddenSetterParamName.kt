// SKIP_DESERIALIZED_IR_TEXT_DUMP
// REASON: KT-69587 Multi-module is not deserialized in JS irText

// MODULE: m1
abstract class Base<T>(val x: T) {
    abstract var bar: T
}

// MODULE: m2(m1)
class Derived1<T>(x: T) : Base<T>(x) {
    override var bar: T = x
}
