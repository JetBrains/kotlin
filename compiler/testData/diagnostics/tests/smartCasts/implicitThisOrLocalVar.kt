// RUN_PIPELINE_TILL: FRONTEND
class Box(var item: String?)

fun expectString(it: String) {}

fun Box.test() {
    val other = Box("")
    myRun {
        if (item != null) {
            expectString(<!SMARTCAST_IMPOSSIBLE!>item<!>)
            other.item = null
            expectString(<!SMARTCAST_IMPOSSIBLE!>item<!>)
            this.item = null
            expectString(<!TYPE_MISMATCH!>item<!>)
        }
    }

    var item: String? = "OK"
    myRun {
        if (item != null) {
            this.item = null
            expectString(<!DEBUG_INFO_SMARTCAST!>item<!>)
        }
    }
}

fun myRun(block: () -> Unit) {}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, equalityExpression, funWithExtensionReceiver, functionDeclaration,
functionalType, ifExpression, lambdaLiteral, localProperty, nullableType, primaryConstructor, propertyDeclaration,
smartcast, stringLiteral, thisExpression */
