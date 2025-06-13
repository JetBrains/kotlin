// RUN_PIPELINE_TILL: BACKEND
open class A {
    open var value: Int = 4
        protected set
}

class MutableA : A() {
    override var value: Int = 4
        public set
}

fun test(myA: A) {
    if (myA is MutableA) {
        <!DEBUG_INFO_SMARTCAST!>myA<!>.value = 5
    }
}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, functionDeclaration, ifExpression, integerLiteral, isExpression,
override, propertyDeclaration, smartcast */
