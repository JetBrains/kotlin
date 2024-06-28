// FIR_IDENTICAL
// LANGUAGE:+TakeIntoAccountEffectivelyFinalInMustBeInitializedCheck
open class Base {
    open var x: String = ""
}

class Foo : Base() {
    override var x: String

    init {
        x = ""
    }
}
