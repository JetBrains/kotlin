// FIR_IDENTICAL
// LANGUAGE:-TakeIntoAccountEffectivelyFinalInMustBeInitializedCheck
open class Base {
    open var x: String = ""
}

class Foo : Base() {
    <!MUST_BE_INITIALIZED_OR_FINAL_OR_ABSTRACT!>override var x: String<!>

    init {
        x = ""
    }
}
