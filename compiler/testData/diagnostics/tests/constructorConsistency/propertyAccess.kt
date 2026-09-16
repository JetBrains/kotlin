// RUN_PIPELINE_TILL: CODEGEN
class My {
    val x: Int

    constructor(x: Int) {
        this.x = x
    }
}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, propertyDeclaration, secondaryConstructor, thisExpression */
