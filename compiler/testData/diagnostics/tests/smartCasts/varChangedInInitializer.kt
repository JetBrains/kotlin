// RUN_PIPELINE_TILL: CODEGEN
class My {
    init {
        var y: Int?
        y = 42
        y.hashCode()
    }
}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, init, integerLiteral, localProperty, nullableType,
propertyDeclaration, smartcast */
