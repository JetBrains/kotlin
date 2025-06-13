// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
object Host {
    val `____` = { -> }
    fun testFunTypeVal() {
        <!UNDERSCORE_USAGE_WITHOUT_BACKTICKS!>____<!>()
    }
}

/* GENERATED_FIR_TAGS: functionDeclaration, lambdaLiteral, objectDeclaration, propertyDeclaration */
