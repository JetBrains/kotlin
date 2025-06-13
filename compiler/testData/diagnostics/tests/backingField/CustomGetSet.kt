// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
class Flower() {

    var minusOne: Int = 1
        get() = field + 1
        set(n: Int) { field = n - 1 }
}

/* GENERATED_FIR_TAGS: additiveExpression, assignment, classDeclaration, getter, integerLiteral, primaryConstructor,
propertyDeclaration, setter */
