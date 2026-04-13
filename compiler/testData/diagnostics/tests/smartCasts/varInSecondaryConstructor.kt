// RUN_PIPELINE_TILL: BACKEND
class My(val z: Int) {
    var x: Int = 0
    constructor(arg: Int?): this(arg ?: 42) {
        var y: Int?
        y = arg
        if (y != null) {
            x = y
        }
    }
}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, elvisExpression, equalityExpression, ifExpression, integerLiteral,
localProperty, nullableType, primaryConstructor, propertyDeclaration, secondaryConstructor, smartcast */
