// RUN_PIPELINE_TILL: FRONTEND
// WITH_EXTRA_CHECKERS
// ISSUE: KT-77041
// IGNORE_PHASE_VERIFICATION: invalid code inside annotations

annotation class Anno(val i: Int)

@Anno(i = <!ANNOTATION_ARGUMENT_MUST_BE_CONST!>{
    class Local {
        fun foo() = 1
        private fun bar() = 2
    }
    fun localFun() = Local().foo()
    localFun()
}()<!>)
class Check

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, functionDeclaration, integerLiteral, lambdaLiteral,
localClass, localFunction, primaryConstructor, propertyDeclaration */
