// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-68521

class Test {
    var x = 10
        private set
}

fun main() {
    val test = Test()

    <!INVISIBLE_SETTER!>test.x<!> = 5
    <!INVISIBLE_SETTER!>test.x<!> -= 5
    <!INVISIBLE_SETTER!>test.x<!>--
    --<!INVISIBLE_SETTER!>test.x<!>
}

/* GENERATED_FIR_TAGS: additiveExpression, assignment, classDeclaration, functionDeclaration,
incrementDecrementExpression, integerLiteral, localProperty, propertyDeclaration */
