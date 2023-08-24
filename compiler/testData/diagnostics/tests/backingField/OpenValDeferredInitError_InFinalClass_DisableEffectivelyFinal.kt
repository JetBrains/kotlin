// FIR_IDENTICAL
// DIAGNOSTICS: -DEBUG_INFO_LEAKING_THIS
// LANGUAGE:-TakeIntoAccountEffectivelyFinalInMustBeInitializedCheck
// LANGUAGE:+ProhibitOpenValDeferredInitialization

// Invalid combination of language feature flags. The user enabled 2.0 language flag (+ProhibitOpenValDeferredInitialization) and disabled
// 1.9 language flag (-TakeIntoAccountEffectivelyFinalInMustBeInitializedCheck) => It's only possible if users manually set `-XXLanguage` =>
// Technically, it's UB (undefined behaviour)
//
// On one hand, suppose that you have 1.9.0 compiler with -XXLanguage:-TakeIntoAccountEffectivelyFinalInMustBeInitializedCheck and then you
// bump your compiler version to 1.9.20. It's not nice that you will get this compilation error considering that you didn't have a warning
// => so probably we shouldn't report a error here
//
// But on the other hand, the error is expected by the combination of feature flags (we prohibit open val deferred initialization and
// since we don't take effectively final into account) => we must be report a error
//
// Anyway, it's UB. Users shouldn't manually tune `-XXLanguage`

interface Base {
    val foo: Int
}

class Foo : Base {
    <!MUST_BE_INITIALIZED_OR_FINAL_OR_ABSTRACT!>override val foo: Int<!>

    init {
        foo = 1
    }
}
