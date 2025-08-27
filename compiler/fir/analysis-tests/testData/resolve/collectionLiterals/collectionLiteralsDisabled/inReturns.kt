// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-79330
// LANGUAGE: -CollectionLiterals

class MyList {
    companion object {
        <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun of(vararg args: String): MyList = MyList()
    }
}

class Irrelevant

fun runLike(block: () -> MyList) = block()

fun testList(): MyList {
    return <!RETURN_TYPE_MISMATCH, UNSUPPORTED!>[]<!>
}
fun testIrrelevant(): Irrelevant = <!RETURN_TYPE_MISMATCH, UNSUPPORTED!>["1", "2", "3"]<!>
fun test() = <!UNSUPPORTED!>[1, 2, 3]<!>
fun testRunLike(): MyList {
    return runLike { <!RETURN_TYPE_MISMATCH, UNSUPPORTED!>["1", "2", "3"]<!> }
}
fun testArrayInt(): Array<Int> = <!UNSUPPORTED!>[1, 2, 3]<!>
fun testIntArray(): IntArray = <!UNSUPPORTED!>[1, 2, 3]<!>

/* GENERATED_FIR_TAGS: classDeclaration, collectionLiteral, companionObject, functionDeclaration, functionalType,
integerLiteral, lambdaLiteral, objectDeclaration, operator, stringLiteral, vararg */
