// RUN_PIPELINE_TILL: CODEGEN
val my: Int = 21
    get() = field * 2

/* GENERATED_FIR_TAGS: getter, integerLiteral, multiplicativeExpression, propertyDeclaration */
