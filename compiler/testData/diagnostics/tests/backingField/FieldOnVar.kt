// RUN_PIPELINE_TILL: CODEGEN
var my: Int = 0
    get() = -field
    set(arg) {
        field = arg
    }

/* GENERATED_FIR_TAGS: assignment, getter, integerLiteral, propertyDeclaration, setter, unaryExpression */
