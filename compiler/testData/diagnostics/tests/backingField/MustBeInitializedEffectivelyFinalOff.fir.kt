// LANGUAGE:-TakeIntoAccountEffectivelyFinalInMustBeInitializedCheck
open class Base {
    open var x: String = ""
}

class Foo : Base() {
    override <!MUST_BE_INITIALIZED_OR_FINAL_OR_ABSTRACT!>var x: String<!>

    init {
        x = ""
    }
}
