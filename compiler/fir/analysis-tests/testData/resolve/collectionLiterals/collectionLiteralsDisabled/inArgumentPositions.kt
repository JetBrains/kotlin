// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-79330
// LANGUAGE: -CollectionLiterals

class MyList {
    companion object {
        <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun of(vararg args: String): MyList = MyList()
    }
}

class Irrelevant

fun takeList(lst: MyList) { }
fun takeIrrelevant(lst: Irrelevant) { }
fun takeArrayInt(lst: Array<Int>) { }
fun takeIntArray(lst: IntArray) { }

fun test() {
    <!UNRESOLVED_REFERENCE!>takeLst<!>(<!UNSUPPORTED_ARRAY_LITERAL_OUTSIDE_OF_ANNOTATION_ERROR!>[]<!>)
    <!UNRESOLVED_REFERENCE!>takeLst<!>(<!UNSUPPORTED_ARRAY_LITERAL_OUTSIDE_OF_ANNOTATION_ERROR!>["1", "2", "3"]<!>)
    <!UNRESOLVED_REFERENCE!>takeLst<!>(<!UNSUPPORTED_ARRAY_LITERAL_OUTSIDE_OF_ANNOTATION_ERROR!>[1, 2, 3]<!>)
    takeIrrelevant(<!ARGUMENT_TYPE_MISMATCH, UNSUPPORTED_ARRAY_LITERAL_OUTSIDE_OF_ANNOTATION_ERROR!>[]<!>)
    takeIrrelevant(<!ARGUMENT_TYPE_MISMATCH, UNSUPPORTED_ARRAY_LITERAL_OUTSIDE_OF_ANNOTATION_ERROR!>[1, 2, 3]<!>)
    takeIntArray(<!ARGUMENT_TYPE_MISMATCH, UNSUPPORTED_ARRAY_LITERAL_OUTSIDE_OF_ANNOTATION_ERROR!>[1, 2, 3]<!>)
    takeArrayInt(<!UNSUPPORTED_ARRAY_LITERAL_OUTSIDE_OF_ANNOTATION_ERROR!>[1, 2, 3]<!>)
}

/* GENERATED_FIR_TAGS: classDeclaration, collectionLiteral, companionObject, functionDeclaration, integerLiteral,
objectDeclaration, operator, stringLiteral, vararg */
