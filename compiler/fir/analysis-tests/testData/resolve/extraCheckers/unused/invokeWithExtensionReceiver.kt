// RUN_PIPELINE_TILL: FRONTEND
fun foo() {
    val ext: String.(Int) -> Unit

    val usedReceiver = "foo"

    val <!UNUSED_VARIABLE!>unusedReceiver<!> = "bar"

    usedReceiver.<!UNINITIALIZED_VARIABLE!>ext<!>(10)
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, integerLiteral, localProperty, propertyDeclaration,
stringLiteral, typeWithExtension */
