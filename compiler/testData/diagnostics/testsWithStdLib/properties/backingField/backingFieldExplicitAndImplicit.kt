// RUN_PIPELINE_TILL: FRONTEND

val x1: Any
    field = <!UNRESOLVED_REFERENCE!>field<!> + mutableListOf("a", "b", "c")

/* GENERATED_FIR_TAGS: additiveExpression, explicitBackingField, propertyDeclaration, stringLiteral */
