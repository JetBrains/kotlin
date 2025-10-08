// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
package usage

class MyClass1
open class ABC {
    open val nestedBlocks = ArrayList<MyClass1>()

    fun makeInjectionBlocks() {
        val l: List<Any> = listOf(1)
        for (b in l) {
            when (b) {
                is Int -> <!VAL_REASSIGNMENT!>nestedBlocks<!> <!ASSIGNMENT_TYPE_MISMATCH!>+=<!> b
                else -> {}
            }
        }
    }
}

/* GENERATED_FIR_TAGS: additiveExpression, assignment, classDeclaration, forLoop, functionDeclaration, integerLiteral,
isExpression, localProperty, propertyDeclaration, smartcast, whenExpression, whenWithSubject */
