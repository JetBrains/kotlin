// FIR_IDENTICAL
// DIAGNOSTICS: -DEBUG_INFO_LEAKING_THIS
// LANGUAGE:-TakeIntoAccountEffectivelyFinalInMustBeInitializedCheck
// LANGUAGE:-ProhibitOpenValDeferredInitialization
// KT-61228 case
interface Base {
    val foo: Int
}

class Foo : Base {
    override val foo: Int

    init {
        foo = 1
    }
}
