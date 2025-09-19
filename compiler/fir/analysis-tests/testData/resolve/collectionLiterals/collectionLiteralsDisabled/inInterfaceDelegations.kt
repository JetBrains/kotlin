// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-79330
// LANGUAGE: -CollectionLiterals

interface MyList {
    companion object {
        <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun of(vararg args: String): MyList = <!INTERFACE_AS_FUNCTION!>MyList<!>()
    }
}

class Impl1: MyList by <!TYPE_MISMATCH, UNSUPPORTED_ARRAY_LITERAL_OUTSIDE_OF_ANNOTATION_ERROR!>[1, 2, 3]<!>
class Impl2: MyList by <!TYPE_MISMATCH, UNSUPPORTED_ARRAY_LITERAL_OUTSIDE_OF_ANNOTATION_ERROR!>["1", "2", "3"]<!>
class Impl3: MyList by <!TYPE_MISMATCH, UNSUPPORTED_ARRAY_LITERAL_OUTSIDE_OF_ANNOTATION_ERROR!>[]<!>

fun test(): MyList {
    return object : MyList by <!TYPE_MISMATCH, UNSUPPORTED_ARRAY_LITERAL_OUTSIDE_OF_ANNOTATION_ERROR!>["1", "2", "3"]<!> { }
}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, classDeclaration, collectionLiteral, companionObject,
functionDeclaration, inheritanceDelegation, integerLiteral, interfaceDeclaration, objectDeclaration, operator,
stringLiteral, vararg */
