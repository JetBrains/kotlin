// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
class Raise() {
    var zz = 1
        get() = field * 2
}

/* GENERATED_FIR_TAGS: classDeclaration, getter, integerLiteral, multiplicativeExpression, primaryConstructor,
propertyDeclaration */
