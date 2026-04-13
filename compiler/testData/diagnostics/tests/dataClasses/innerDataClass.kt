// RUN_PIPELINE_TILL: FRONTEND
class Outer {
    <!INCOMPATIBLE_MODIFIERS!>inner<!> <!INCOMPATIBLE_MODIFIERS!>data<!> class Inner(val x: Int)
}

/* GENERATED_FIR_TAGS: classDeclaration, data, inner, primaryConstructor, propertyDeclaration */
