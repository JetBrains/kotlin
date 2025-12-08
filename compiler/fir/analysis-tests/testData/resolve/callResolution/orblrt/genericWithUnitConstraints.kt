// RUN_PIPELINE_TILL: FRONTEND

fun <T> bar(x: String, y: Boolean = false, block: () -> T): T = TODO()

fun <T> bar(x: String, block: () -> T): T = TODO()

fun foo(): Any? = null

fun main() {
    bar<Unit>("") {
        <!RETURN_TYPE_MISMATCH!>foo()<!>
    }

    bar<Unit>("") {
        <!RETURN_TYPE_MISMATCH!>try {
            foo()
        } catch (e: Throwable) {
            if (e.hashCode() == 0) {
                throw e
            }
        }<!>
    }
}

/* GENERATED_FIR_TAGS: equalityExpression, functionDeclaration, functionalType, ifExpression, integerLiteral,
lambdaLiteral, localProperty, nullableType, propertyDeclaration, stringLiteral, tryExpression, typeParameter */
