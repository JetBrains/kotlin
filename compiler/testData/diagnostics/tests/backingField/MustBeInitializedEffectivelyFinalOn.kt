// RUN_PIPELINE_TILL: BACKEND
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

/* GENERATED_FIR_TAGS: assignment, classDeclaration, init, override, propertyDeclaration, stringLiteral */
