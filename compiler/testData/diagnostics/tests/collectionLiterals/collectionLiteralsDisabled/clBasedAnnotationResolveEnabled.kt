// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: -CollectionLiterals
// LANGUAGE: +CollectionLiteralsBasedAnnotationResolution
// WITH_STDLIB

annotation class Anno(val arr: Array<String> = ["!"])
class NonAnno(itr: Iterable<String> = <!UNSUPPORTED_FEATURE!>["1", "2", "3"]<!>)

@Anno([])
fun test(): String {
    val set: Set<Int> = <!UNSUPPORTED_FEATURE!>[42]<!>
    val custom: Custom = run {
        <!UNSUPPORTED_FEATURE!>[1, 2, 3]<!>
    }
    for (element in <!UNSUPPORTED_FEATURE!>[1, 2, 3]<!>) {
        element + 42
    }
    return <!RETURN_TYPE_MISMATCH, UNSUPPORTED_FEATURE!>['a', 'b', 'c']<!>
}

class Custom {
    companion object {
        <!UNSUPPORTED_FEATURE!>operator<!> fun of(vararg xs: Int): Custom = Custom()
    }
}

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, collectionLiteral, companionObject, functionDeclaration,
integerLiteral, lambdaLiteral, localProperty, objectDeclaration, operator, primaryConstructor, propertyDeclaration,
stringLiteral, vararg */
