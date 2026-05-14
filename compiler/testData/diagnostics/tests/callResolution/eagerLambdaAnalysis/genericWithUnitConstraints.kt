// RUN_PIPELINE_TILL: BACKEND

// This file doesn't contain LANGUAGE directive because it's expected
// to work independently of language-version-settings configuration, so there should be no LATEST_LV_DIFFERENCE.

fun <T> bar(x: String, y: Boolean = false, block: () -> T): T = TODO()

fun <T> bar(x: String, block: () -> T): T = TODO()

fun foo(): Any? = null

fun main() {
    bar<Unit>("") {
        foo()
    }

    bar<Unit>("") {
        try {
            foo()
        } catch (e: Throwable) {
            if (e.hashCode() == 0) {
                throw e
            }
        }
    }
}

/* GENERATED_FIR_TAGS: equalityExpression, functionDeclaration, functionalType, ifExpression, integerLiteral,
lambdaLiteral, localProperty, nullableType, propertyDeclaration, stringLiteral, tryExpression, typeParameter */
