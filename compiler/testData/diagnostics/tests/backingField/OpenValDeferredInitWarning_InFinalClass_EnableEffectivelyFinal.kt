// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// DIAGNOSTICS: -DEBUG_INFO_LEAKING_THIS
// LANGUAGE:+TakeIntoAccountEffectivelyFinalInMustBeInitializedCheck
// LANGUAGE:-ProhibitOpenValDeferredInitialization
interface Base {
    val foo: Int
}

class Foo : Base {
    override val foo: Int

    init {
        foo = 1
    }
}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, init, integerLiteral, interfaceDeclaration, override,
propertyDeclaration */
