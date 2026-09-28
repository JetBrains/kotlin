// RUN_PIPELINE_TILL: CODEGEN
var myProperty = listOf(1, 2, 3)
    get() {
        return field + field
    }
    set(param) {
        field = param
    }

/* GENERATED_FIR_TAGS: additiveExpression, assignment, getter, integerLiteral, propertyDeclaration, setter */
