// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_EXPRESSION

val i: Int = 10
    get() {
        ::<!UNSUPPORTED!>field<!>
        return field
    }

/* GENERATED_FIR_TAGS: getter, integerLiteral, propertyDeclaration */
