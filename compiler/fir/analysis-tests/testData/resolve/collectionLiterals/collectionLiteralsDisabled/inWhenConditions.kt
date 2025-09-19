// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-79330
// LANGUAGE: -CollectionLiterals

class MyList {
    companion object {
        <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun of(vararg args: String): MyList = MyList()
    }
}

fun test(lst: MyList) {
    when(lst) {
        <!UNSUPPORTED_ARRAY_LITERAL_OUTSIDE_OF_ANNOTATION_ERROR!>["1", "2", "3"]<!> -> { }
        <!UNSUPPORTED_ARRAY_LITERAL_OUTSIDE_OF_ANNOTATION_ERROR!>[]<!> -> { }
        <!UNSUPPORTED_ARRAY_LITERAL_OUTSIDE_OF_ANNOTATION_ERROR!>[1, 2, 3]<!> -> { }
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, collectionLiteral, companionObject, equalityExpression, functionDeclaration,
integerLiteral, objectDeclaration, operator, stringLiteral, vararg, whenExpression, whenWithSubject */
