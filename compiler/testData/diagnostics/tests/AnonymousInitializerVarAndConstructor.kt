// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// http://youtrack.jetbrains.net/issue/KT-419

class A(w: Int) {
    var c = w

    init {
        c = 81
    }
}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, init, integerLiteral, primaryConstructor, propertyDeclaration */
