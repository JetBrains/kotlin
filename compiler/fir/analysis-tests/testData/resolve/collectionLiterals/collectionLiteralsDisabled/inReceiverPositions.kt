// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-79330
// LANGUAGE: -CollectionLiterals
// RENDER_DIAGNOSTIC_ARGUMENTS

class MyList {
    companion object {
        <!UNSUPPORTED_FEATURE("The feature \"collection literals\" is experimental and should be enabled explicitly. This can be done by supplying the compiler argument '-XXLanguage:+CollectionLiterals', but note that no stability guarantees are provided.")!>operator<!> fun of(vararg args: String): MyList = MyList()
    }
}

class Irrelevant

fun MyList.takeList() { }
fun Irrelevant.takeIrrelevant() { }
fun IntArray.takeIntArray() { }
fun Array<Int>.takeArrayInt() { }

fun test() {
    <!UNSUPPORTED_ARRAY_LITERAL_OUTSIDE_OF_ANNOTATION_ERROR!>[]<!>.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER("fun MyList.takeList(): Unit")!>takeList<!>()
    <!UNSUPPORTED_ARRAY_LITERAL_OUTSIDE_OF_ANNOTATION_ERROR!>["1", "2", "3"]<!>.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER("fun MyList.takeList(): Unit")!>takeList<!>()
    <!UNSUPPORTED_ARRAY_LITERAL_OUTSIDE_OF_ANNOTATION_ERROR!>[1, 2, 3]<!>.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER("fun MyList.takeList(): Unit")!>takeList<!>()
    <!UNSUPPORTED_ARRAY_LITERAL_OUTSIDE_OF_ANNOTATION_ERROR!>[]<!>.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER("fun Irrelevant.takeIrrelevant(): Unit")!>takeIrrelevant<!>()
    <!UNSUPPORTED_ARRAY_LITERAL_OUTSIDE_OF_ANNOTATION_ERROR!>[1, 2, 3]<!>.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER("fun Irrelevant.takeIrrelevant(): Unit")!>takeIrrelevant<!>()
    <!UNSUPPORTED_ARRAY_LITERAL_OUTSIDE_OF_ANNOTATION_ERROR!>[1, 2, 3]<!>.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER("fun IntArray.takeIntArray(): Unit")!>takeIntArray<!>()
    <!UNSUPPORTED_ARRAY_LITERAL_OUTSIDE_OF_ANNOTATION_ERROR!>[1, 2, 3]<!>.takeArrayInt()
}

/* GENERATED_FIR_TAGS: classDeclaration, collectionLiteral, companionObject, funWithExtensionReceiver,
functionDeclaration, integerLiteral, objectDeclaration, operator, stringLiteral, vararg */
