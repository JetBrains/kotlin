// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +FullValueClasses
// WITH_STDLIB

value object Constants {
    const <!PROPERTY_WITH_BACKING_FIELD_INSIDE_VALUE_CLASS!>val X<!> = 1
}

/* GENERATED_FIR_TAGS: const, integerLiteral, objectDeclaration, propertyDeclaration, value */
