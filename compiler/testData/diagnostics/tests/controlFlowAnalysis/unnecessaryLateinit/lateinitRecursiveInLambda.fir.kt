// RUN_PIPELINE_TILL: BACKEND
class Test {
    <!UNNECESSARY_LATEINIT!>lateinit<!> var someRunnable: Runnable
    init {
        someRunnable = Runnable { someRunnable.run() }
    }
}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, init, javaFunction, lambdaLiteral, lateinit, propertyDeclaration */
