// RUN_PIPELINE_TILL: FRONTEND
interface I {
    val it: Number
        <!EXPLICIT_BACKING_FIELD_IN_INTERFACE!>field<!> = 10
}

/* GENERATED_FIR_TAGS: explicitBackingField, integerLiteral, interfaceDeclaration, propertyDeclaration */
