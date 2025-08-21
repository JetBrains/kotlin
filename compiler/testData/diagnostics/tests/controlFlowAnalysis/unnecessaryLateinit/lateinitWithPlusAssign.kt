// FIR_IDENTICAL
// RUN_PIPELINE_TILL: BACKEND
class Foo {
    lateinit var bar: String

    constructor(baz: Int) {
        // At best, we should have error here despite of lateinit
        bar += baz
    }
}

/* GENERATED_FIR_TAGS: additiveExpression, assignment, classDeclaration, lateinit, propertyDeclaration,
secondaryConstructor */
