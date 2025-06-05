// RUN_PIPELINE_TILL: FRONTEND
// K2: See KT-65342

fun test() {
    a@ b@ while(true) {
        val f = {
            <!NOT_A_FUNCTION_LABEL!>return@a<!>
        }
        break@b
    }
}

/* GENERATED_FIR_TAGS: break, functionDeclaration, lambdaLiteral, localProperty, propertyDeclaration, whileLoop */
