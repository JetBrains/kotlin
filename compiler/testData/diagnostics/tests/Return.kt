// RUN_PIPELINE_TILL: FRONTEND
package <!SYNTAX!>return<!>

class A {
    fun outer() {
        fun inner() {
            if (1 < 2)
                return@inner
            else
            <!RETURN_NOT_ALLOWED!>return@outer<!>
        }
        if (1 < 2)
            return<!UNRESOLVED_REFERENCE!>@A<!>
        else if (2 < 3)
            return<!UNRESOLVED_REFERENCE!>@inner<!>
        return@outer
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, comparisonExpression, functionDeclaration, ifExpression, integerLiteral,
localFunction */
