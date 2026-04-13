// RUN_PIPELINE_TILL: BACKEND
class Raise() {
    var zz = 1
        set(it) { field = it / 2 }
}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, integerLiteral, multiplicativeExpression, primaryConstructor,
propertyDeclaration, setter */
