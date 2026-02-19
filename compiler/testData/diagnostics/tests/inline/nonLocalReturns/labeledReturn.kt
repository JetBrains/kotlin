// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
fun inlineCall(): String {
    inlineFun {
        if (true) {
            return@inlineCall ""
        }
        1
    }

    return "x"
}

inline fun inlineFun(s: ()->Int) {
    s()
}


fun noInlineCall(): String {
    noInline {
        if (true) {
            <!RETURN_NOT_ALLOWED!>return@noInlineCall<!> ""
        }
        1
    }

    return "x"
}


fun noInline(s: ()->Int) {
    s()
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, ifExpression, inline, integerLiteral, lambdaLiteral,
stringLiteral */
