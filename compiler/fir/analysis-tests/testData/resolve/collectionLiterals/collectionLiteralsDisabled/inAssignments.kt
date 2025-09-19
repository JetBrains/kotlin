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

fun test() {
    var x: MyList = <!INITIALIZER_TYPE_MISMATCH, UNSUPPORTED!>[1, 2, 3]<!>
    x = <!ASSIGNMENT_TYPE_MISMATCH, UNSUPPORTED!>[]<!>
    x = <!ASSIGNMENT_TYPE_MISMATCH, UNSUPPORTED!>["1", "2", "3"]<!>
    val y: Irrelevant = <!INITIALIZER_TYPE_MISMATCH, UNSUPPORTED!>["1", "2", "3"]<!>
    val z = <!UNSUPPORTED!>[1, 2, 3]<!>
    val t: Array<Int> = <!UNSUPPORTED!>[1, 2, 3]<!>
}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, collectionLiteral, companionObject, functionDeclaration,
integerLiteral, localProperty, objectDeclaration, operator, propertyDeclaration, stringLiteral, vararg */
