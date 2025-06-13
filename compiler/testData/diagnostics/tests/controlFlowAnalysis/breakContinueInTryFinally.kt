// RUN_PIPELINE_TILL: BACKEND
fun foo() {
    outer@while (true) {
        try {
            while (true) {
                <!UNREACHABLE_CODE!>continue@outer<!>
            }
        } finally {
            break
        }
    }
    "OK".hashCode()
}

fun bar(): String {
    outer@while (true) {
        try {
            while (true) {
                <!UNREACHABLE_CODE!>continue@outer<!>
            }
        } finally {
            return "OK"
        }
    }
}

/* GENERATED_FIR_TAGS: break, continue, functionDeclaration, stringLiteral, tryExpression, whileLoop */
