// RUN_PIPELINE_TILL: CODEGEN
class CustomGetVal() {
    val zz = 1
        get() = field * 2
}

/* GENERATED_FIR_TAGS: classDeclaration, getter, integerLiteral, multiplicativeExpression, primaryConstructor,
propertyDeclaration */
