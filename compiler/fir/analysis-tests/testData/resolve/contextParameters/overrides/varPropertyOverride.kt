// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +ContextParameters
// ISSUE: KT-58165

abstract class Base {
    context(_: String)
    abstract var i: Any
}

class Derived : Base() {
    context(_: String)
    override var i: Any
        get() = ""
        set(value) {}
}

/* GENERATED_FIR_TAGS: classDeclaration, getter, override, propertyDeclaration, propertyDeclarationWithContext, setter,
stringLiteral */
