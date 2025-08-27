// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-79330
// LANGUAGE: -CollectionLiterals

class MyList {
    companion object {
        <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun of(vararg args: String): MyList = MyList()
    }
}

class Irrelevant

fun MyList.takeList() { }
fun Irrelevant.takeIrrelevant() { }
fun IntArray.takeIntArray() { }
fun Array<Int>.takeArrayInt() { }

fun test() {
    <!UNSUPPORTED!>[]<!>.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>takeList<!>()
    <!UNSUPPORTED!>["1", "2", "3"]<!>.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>takeList<!>()
    <!UNSUPPORTED!>[1, 2, 3]<!>.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>takeList<!>()
    <!UNSUPPORTED!>[]<!>.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>takeIrrelevant<!>()
    <!UNSUPPORTED!>[1, 2, 3]<!>.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>takeIrrelevant<!>()
    <!UNSUPPORTED!>[1, 2, 3]<!>.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>takeIntArray<!>()
    <!UNSUPPORTED!>[1, 2, 3]<!>.takeArrayInt()
}

/* GENERATED_FIR_TAGS: classDeclaration, collectionLiteral, companionObject, funWithExtensionReceiver,
functionDeclaration, integerLiteral, objectDeclaration, operator, stringLiteral, vararg */
